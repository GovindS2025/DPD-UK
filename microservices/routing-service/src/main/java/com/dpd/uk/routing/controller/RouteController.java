package com.dpd.uk.routing.controller;

import com.dpd.uk.common.model.Address;
import com.dpd.uk.common.model.Route;
import com.dpd.uk.routing.model.RouteOptimizationRequest;
import com.dpd.uk.routing.model.RouteOptimizationResult;
import com.dpd.uk.routing.service.RouteOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {
    
    private final RouteOptimizationService routeOptimizationService;
    
    @PostMapping("/optimize")
    public ResponseEntity<RouteOptimizationResult> optimizeRoute(@Valid @RequestBody RouteOptimizationRequest request) {
        log.info("Received route optimization request for depot: {}, driver: {}", 
            request.getDepotId(), request.getDriverId());
        
        try {
            RouteOptimizationResult result = routeOptimizationService.optimizeRoute(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error optimizing route for depot: {}", request.getDepotId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{routeId}")
    public ResponseEntity<Route> getRoute(@PathVariable String routeId) {
        log.info("Received route request for route: {}", routeId);
        
        Route route = routeOptimizationService.getRoute(routeId);
        return route != null ? ResponseEntity.ok(route) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/depot/{depotId}")
    public ResponseEntity<List<Route>> getRoutesByDepot(@PathVariable String depotId) {
        log.info("Received routes request for depot: {}", depotId);
        
        List<Route> routes = routeOptimizationService.getRoutesByDepot(depotId);
        return ResponseEntity.ok(routes);
    }
    
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Route>> getRoutesByDriver(@PathVariable String driverId) {
        log.info("Received routes request for driver: {}", driverId);
        
        List<Route> routes = routeOptimizationService.getRoutesByDriver(driverId);
        return ResponseEntity.ok(routes);
    }
    
    @PutMapping("/{routeId}/status")
    public ResponseEntity<Route> updateRouteStatus(@PathVariable String routeId, 
                                                 @RequestParam Route.RouteStatus status) {
        log.info("Received route status update request for route: {}, status: {}", routeId, status);
        
        try {
            Route route = routeOptimizationService.updateRouteStatus(routeId, status);
            return route != null ? ResponseEntity.ok(route) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating route status for route: {}", routeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{routeId}/reroute")
    public ResponseEntity<Route> reroute(@PathVariable String routeId, 
                                       @RequestBody List<Address> newStops) {
        log.info("Received reroute request for route: {} with {} stops", routeId, newStops.size());
        
        try {
            Route route = routeOptimizationService.reroute(routeId, newStops);
            return route != null ? ResponseEntity.ok(route) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error rerouting route: {}", routeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        return ResponseEntity.ok(java.util.Map.of(
            "status", "UP",
            "service", "routing-service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
