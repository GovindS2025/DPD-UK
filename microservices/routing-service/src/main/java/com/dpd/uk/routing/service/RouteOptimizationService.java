package com.dpd.uk.routing.service;

import com.dpd.uk.common.model.Address;
import com.dpd.uk.common.model.Parcel;
import com.dpd.uk.common.model.Route;
import com.dpd.uk.routing.model.RouteOptimizationRequest;
import com.dpd.uk.routing.model.RouteOptimizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteOptimizationService {
    
    private final MappingService mappingService;
    private final RouteRepository routeRepository;
    private final RouteNotificationService routeNotificationService;
    private final GeneticAlgorithmOptimizer geneticAlgorithmOptimizer;
    private final SimulatedAnnealingOptimizer simulatedAnnealingOptimizer;
    private final TabuSearchOptimizer tabuSearchOptimizer;
    private final GreedyOptimizer greedyOptimizer;
    
    @Cacheable(value = "optimized-routes", key = "#request.depotId + '_' + #request.driverId + '_' + #request.algorithm")
    public RouteOptimizationResult optimizeRoute(RouteOptimizationRequest request) {
        log.info("Starting route optimization for depot: {}, driver: {}, algorithm: {}", 
            request.getDepotId(), request.getDriverId(), request.getAlgorithm());
        
        try {
            // Validate request
            validateOptimizationRequest(request);
            
            // Get distance matrix for all stops
            CompletableFuture<Map<String, Map<String, Double>>> distanceMatrixFuture = 
                CompletableFuture.supplyAsync(() -> mappingService.calculateDistanceMatrix(request.getStops()));
            
            // Get time matrix for all stops
            CompletableFuture<Map<String, Map<String, Integer>>> timeMatrixFuture = 
                CompletableFuture.supplyAsync(() -> mappingService.calculateTimeMatrix(request.getStops()));
            
            // Wait for both matrices
            CompletableFuture.allOf(distanceMatrixFuture, timeMatrixFuture).join();
            
            Map<String, Map<String, Double>> distanceMatrix = distanceMatrixFuture.get();
            Map<String, Map<String, Integer>> timeMatrix = timeMatrixFuture.get();
            
            // Run optimization algorithm
            RouteOptimizationResult result = runOptimizationAlgorithm(request, distanceMatrix, timeMatrix);
            
            // Create optimized route
            Route optimizedRoute = createOptimizedRoute(request, result);
            
            // Save route
            routeRepository.save(optimizedRoute);
            
            // Notify about optimization completion
            routeNotificationService.notifyRouteOptimized(optimizedRoute);
            
            log.info("Route optimization completed for depot: {}, route: {}", 
                request.getDepotId(), optimizedRoute.getRouteId());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error optimizing route for depot: {}", request.getDepotId(), e);
            return createFallbackResult(request);
        }
    }
    
    public Route getRoute(String routeId) {
        return routeRepository.findById(routeId).orElse(null);
    }
    
    public List<Route> getRoutesByDepot(String depotId) {
        return routeRepository.findByDepotIdAndStatus(depotId, Route.RouteStatus.PLANNED);
    }
    
    public List<Route> getRoutesByDriver(String driverId) {
        return routeRepository.findByDriverIdAndStatus(driverId, Route.RouteStatus.IN_PROGRESS);
    }
    
    public Route updateRouteStatus(String routeId, Route.RouteStatus status) {
        return routeRepository.findById(routeId)
            .map(route -> {
                route.setStatus(status);
                if (status == Route.RouteStatus.IN_PROGRESS) {
                    route.setActualStartTime(LocalDateTime.now());
                } else if (status == Route.RouteStatus.COMPLETED) {
                    route.setActualEndTime(LocalDateTime.now());
                }
                return routeRepository.save(route);
            })
            .orElse(null);
    }
    
    public Route reroute(String routeId, List<Address> newStops) {
        Route existingRoute = routeRepository.findById(routeId).orElse(null);
        if (existingRoute == null) {
            throw new IllegalArgumentException("Route not found: " + routeId);
        }
        
        // Create new optimization request for rerouting
        RouteOptimizationRequest rerouteRequest = RouteOptimizationRequest.builder()
            .depotId(existingRoute.getDepotId())
            .driverId(existingRoute.getDriverId())
            .vehicleId(existingRoute.getVehicleId())
            .stops(newStops)
            .algorithm("GENETIC_ALGORITHM") // Use genetic algorithm for rerouting
            .maxDurationHours(8)
            .maxDistanceKm(200)
            .build();
        
        // Optimize new route
        RouteOptimizationResult result = optimizeRoute(rerouteRequest);
        
        // Update existing route with new stops
        existingRoute.setStops(convertToRouteStops(newStops, result.getOptimizedSequence()));
        existingRoute.setTotalDistanceKm(result.getTotalDistance());
        existingRoute.setEstimatedDurationMinutes(result.getTotalTime());
        existingRoute.setLastUpdated(LocalDateTime.now());
        existingRoute.setStatus(Route.RouteStatus.OPTIMIZING);
        
        return routeRepository.save(existingRoute);
    }
    
    private void validateOptimizationRequest(RouteOptimizationRequest request) {
        if (request.getStops() == null || request.getStops().isEmpty()) {
            throw new IllegalArgumentException("Stops cannot be empty");
        }
        if (request.getDepotId() == null || request.getDepotId().trim().isEmpty()) {
            throw new IllegalArgumentException("Depot ID is required");
        }
        if (request.getDriverId() == null || request.getDriverId().trim().isEmpty()) {
            throw new IllegalArgumentException("Driver ID is required");
        }
    }
    
    private RouteOptimizationResult runOptimizationAlgorithm(RouteOptimizationRequest request, 
                                                           Map<String, Map<String, Double>> distanceMatrix,
                                                           Map<String, Map<String, Integer>> timeMatrix) {
        return switch (request.getAlgorithm().toUpperCase()) {
            case "GENETIC_ALGORITHM" -> geneticAlgorithmOptimizer.optimize(request, distanceMatrix, timeMatrix);
            case "SIMULATED_ANNEALING" -> simulatedAnnealingOptimizer.optimize(request, distanceMatrix, timeMatrix);
            case "TABU_SEARCH" -> tabuSearchOptimizer.optimize(request, distanceMatrix, timeMatrix);
            case "GREEDY" -> greedyOptimizer.optimize(request, distanceMatrix, timeMatrix);
            default -> {
                log.warn("Unknown algorithm: {}, falling back to genetic algorithm", request.getAlgorithm());
                yield geneticAlgorithmOptimizer.optimize(request, distanceMatrix, timeMatrix);
            }
        };
    }
    
    private Route createOptimizedRoute(RouteOptimizationRequest request, RouteOptimizationResult result) {
        String routeId = "ROUTE_" + System.currentTimeMillis() + "_" + request.getDepotId();
        
        return Route.builder()
            .routeId(routeId)
            .depotId(request.getDepotId())
            .driverId(request.getDriverId())
            .vehicleId(request.getVehicleId())
            .status(Route.RouteStatus.PLANNED)
            .stops(convertToRouteStops(request.getStops(), result.getOptimizedSequence()))
            .totalDistanceKm(result.getTotalDistance())
            .estimatedDurationMinutes(result.getTotalTime())
            .plannedStartTime(LocalDateTime.now().plusHours(1)) // Start in 1 hour
            .plannedEndTime(LocalDateTime.now().plusHours(1).plusMinutes(result.getTotalTime()))
            .optimizationAlgorithm(request.getAlgorithm())
            .metadata(Map.of(
                "optimizationTime", result.getOptimizationTime(),
                "iterations", result.getIterations(),
                "fitness", result.getFitness()
            ))
            .build();
    }
    
    private List<Route.RouteStop> convertToRouteStops(List<Address> addresses, List<Integer> sequence) {
        List<Route.RouteStop> stops = new ArrayList<>();
        
        for (int i = 0; i < sequence.size(); i++) {
            int index = sequence.get(i);
            Address address = addresses.get(index);
            
            Route.RouteStop stop = Route.RouteStop.builder()
                .stopId("STOP_" + i + "_" + System.currentTimeMillis())
                .address(address)
                .type(i == 0 ? Route.RouteStop.StopType.DEPOT : Route.RouteStop.StopType.DELIVERY)
                .sequence(i)
                .status(Route.RouteStop.StopStatus.PENDING)
                .build();
            
            stops.add(stop);
        }
        
        return stops;
    }
    
    private RouteOptimizationResult createFallbackResult(RouteOptimizationRequest request) {
        // Create a simple greedy solution as fallback
        List<Integer> sequence = new ArrayList<>();
        for (int i = 0; i < request.getStops().size(); i++) {
            sequence.add(i);
        }
        
        return RouteOptimizationResult.builder()
            .optimizedSequence(sequence)
            .totalDistance(calculateTotalDistance(request.getStops(), sequence))
            .totalTime(calculateTotalTime(request.getStops(), sequence))
            .optimizationTime(0)
            .iterations(0)
            .fitness(1.0)
            .algorithm("FALLBACK")
            .build();
    }
    
    private double calculateTotalDistance(List<Address> addresses, List<Integer> sequence) {
        // Simplified distance calculation
        double totalDistance = 0.0;
        for (int i = 0; i < sequence.size() - 1; i++) {
            Address from = addresses.get(sequence.get(i));
            Address to = addresses.get(sequence.get(i + 1));
            totalDistance += calculateHaversineDistance(from, to);
        }
        return totalDistance;
    }
    
    private int calculateTotalTime(List<Address> addresses, List<Integer> sequence) {
        // Simplified time calculation (assuming 30 km/h average speed)
        double totalDistance = calculateTotalDistance(addresses, sequence);
        return (int) (totalDistance / 30.0 * 60); // Convert to minutes
    }
    
    private double calculateHaversineDistance(Address from, Address to) {
        final int R = 6371; // Earth's radius in kilometers
        
        double lat1 = Math.toRadians(from.getLatitude());
        double lon1 = Math.toRadians(from.getLongitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double lon2 = Math.toRadians(to.getLongitude());
        
        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;
        
        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dlon / 2) * Math.sin(dlon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
