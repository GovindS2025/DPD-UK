package com.dpd.uk.eta.service;

import com.dpd.uk.common.model.ETA;
import com.dpd.uk.common.model.Parcel;
import com.dpd.uk.eta.model.ETACalculation;
import com.dpd.uk.eta.repository.ETACalculationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ETACalculationService {
    
    private final ETACalculationRepository etaCalculationRepository;
    private final TrafficDataService trafficDataService;
    private final HistoricalDataService historicalDataService;
    private final DepotConstraintsService depotConstraintsService;
    private final VehicleTelematicsService vehicleTelematicsService;
    private final ETANotificationService etaNotificationService;
    
    @Transactional
    public ETA calculateETA(Parcel parcel) {
        log.info("Calculating ETA for parcel: {}", parcel.getParcelId());
        
        try {
            // Get or create ETA calculation
            ETACalculation calculation = getOrCreateCalculation(parcel);
            
            // Gather all factors asynchronously
            CompletableFuture<Map<String, Object>> trafficFactors = 
                CompletableFuture.supplyAsync(() -> trafficDataService.getTrafficFactors(parcel));
            
            CompletableFuture<Map<String, Object>> historicalFactors = 
                CompletableFuture.supplyAsync(() -> historicalDataService.getHistoricalFactors(parcel));
            
            CompletableFuture<Map<String, Object>> depotConstraints = 
                CompletableFuture.supplyAsync(() -> depotConstraintsService.getDepotConstraints(parcel.getDepotId()));
            
            CompletableFuture<Map<String, Object>> vehicleFactors = 
                CompletableFuture.supplyAsync(() -> vehicleTelematicsService.getVehicleFactors(parcel.getVehicleId()));
            
            // Wait for all factors and calculate ETA
            CompletableFuture.allOf(trafficFactors, historicalFactors, depotConstraints, vehicleFactors)
                .thenRun(() -> {
                    calculation.setTrafficFactors(trafficFactors.join());
                    calculation.setHistoricalFactors(historicalFactors.join());
                    calculation.setDepotConstraints(depotConstraints.join());
                    calculation.setVehicleFactors(vehicleFactors.join());
                    
                    // Calculate final ETA
                    calculateFinalETA(calculation);
                    
                    // Save and notify
                    etaCalculationRepository.save(calculation);
                    etaNotificationService.notifyETAUpdate(convertToETA(calculation));
                });
            
            return convertToETA(calculation);
            
        } catch (Exception e) {
            log.error("Error calculating ETA for parcel: {}", parcel.getParcelId(), e);
            return createFallbackETA(parcel);
        }
    }
    
    @Cacheable(value = "eta-calculations", key = "#parcelId")
    public Optional<ETA> getETA(String parcelId) {
        return etaCalculationRepository.findByParcelIdAndIsActiveTrue(parcelId)
            .map(this::convertToETA);
    }
    
    public List<ETA> getETAsByDepot(String depotId) {
        return etaCalculationRepository.findByDepotIdAndIsActiveTrue(depotId)
            .stream()
            .map(this::convertToETA)
            .toList();
    }
    
    public List<ETA> getETAsByDriver(String driverId) {
        return etaCalculationRepository.findByDriverIdAndIsActiveTrue(driverId)
            .stream()
            .map(this::convertToETA)
            .toList();
    }
    
    @Transactional
    public void updateETA(String parcelId, Map<String, Object> updates) {
        etaCalculationRepository.findByParcelIdAndIsActiveTrue(parcelId)
            .ifPresent(calculation -> {
                // Update factors based on real-time data
                if (updates.containsKey("traffic")) {
                    calculation.setTrafficFactors((Map<String, Object>) updates.get("traffic"));
                }
                if (updates.containsKey("vehicle")) {
                    calculation.setVehicleFactors((Map<String, Object>) updates.get("vehicle"));
                }
                
                calculation.setLastUpdated(LocalDateTime.now());
                calculateFinalETA(calculation);
                
                etaCalculationRepository.save(calculation);
                etaNotificationService.notifyETAUpdate(convertToETA(calculation));
            });
    }
    
    private ETACalculation getOrCreateCalculation(Parcel parcel) {
        return etaCalculationRepository.findByParcelIdAndIsActiveTrue(parcel.getParcelId())
            .orElse(ETACalculation.builder()
                .parcelId(parcel.getParcelId())
                .depotId(parcel.getDepotId())
                .driverId(parcel.getDriverId())
                .vehicleId(parcel.getVehicleId())
                .origin(parcel.getOrigin())
                .destination(parcel.getDestination())
                .calculatedAt(LocalDateTime.now())
                .isActive(true)
                .status("CALCULATING")
                .build());
    }
    
    private void calculateFinalETA(ETACalculation calculation) {
        // This is where the actual ETA calculation algorithm would be implemented
        // For now, using a simplified calculation based on distance and factors
        
        double baseTimeMinutes = calculateBaseTime(calculation);
        double trafficMultiplier = getTrafficMultiplier(calculation.getTrafficFactors());
        double historicalMultiplier = getHistoricalMultiplier(calculation.getHistoricalFactors());
        double depotMultiplier = getDepotMultiplier(calculation.getDepotConstraints());
        double vehicleMultiplier = getVehicleMultiplier(calculation.getVehicleFactors());
        
        int finalMinutes = (int) (baseTimeMinutes * trafficMultiplier * historicalMultiplier * depotMultiplier * vehicleMultiplier);
        
        calculation.setEstimatedMinutes(finalMinutes);
        calculation.setEstimatedArrival(LocalDateTime.now().plusMinutes(finalMinutes));
        calculation.setConfidence(determineConfidence(calculation));
        calculation.setLastUpdated(LocalDateTime.now());
        calculation.setStatus("CALCULATED");
    }
    
    private double calculateBaseTime(ETACalculation calculation) {
        // Simplified base time calculation based on distance
        // In reality, this would use sophisticated routing algorithms
        return calculation.getDistanceKm() != null ? calculation.getDistanceKm() * 2.0 : 60.0;
    }
    
    private double getTrafficMultiplier(Map<String, Object> trafficFactors) {
        if (trafficFactors == null) return 1.0;
        return (Double) trafficFactors.getOrDefault("multiplier", 1.0);
    }
    
    private double getHistoricalMultiplier(Map<String, Object> historicalFactors) {
        if (historicalFactors == null) return 1.0;
        return (Double) historicalFactors.getOrDefault("multiplier", 1.0);
    }
    
    private double getDepotMultiplier(Map<String, Object> depotConstraints) {
        if (depotConstraints == null) return 1.0;
        return (Double) depotConstraints.getOrDefault("multiplier", 1.0);
    }
    
    private double getVehicleMultiplier(Map<String, Object> vehicleFactors) {
        if (vehicleFactors == null) return 1.0;
        return (Double) vehicleFactors.getOrDefault("multiplier", 1.0);
    }
    
    private ETA.ETAConfidence determineConfidence(ETACalculation calculation) {
        // Determine confidence based on data quality and recency
        int dataQuality = 0;
        if (calculation.getTrafficFactors() != null) dataQuality++;
        if (calculation.getHistoricalFactors() != null) dataQuality++;
        if (calculation.getDepotConstraints() != null) dataQuality++;
        if (calculation.getVehicleFactors() != null) dataQuality++;
        
        return switch (dataQuality) {
            case 4 -> ETA.ETAConfidence.HIGH;
            case 3 -> ETA.ETAConfidence.MEDIUM;
            case 2 -> ETA.ETAConfidence.LOW;
            default -> ETA.ETAConfidence.VERY_LOW;
        };
    }
    
    private ETA convertToETA(ETACalculation calculation) {
        return ETA.builder()
            .parcelId(calculation.getParcelId())
            .estimatedArrival(calculation.getEstimatedArrival())
            .confidence(calculation.getConfidence())
            .depotId(calculation.getDepotId())
            .driverId(calculation.getDriverId())
            .vehicleId(calculation.getVehicleId())
            .calculatedAt(calculation.getCalculatedAt())
            .lastUpdated(calculation.getLastUpdated())
            .estimatedMinutes(calculation.getEstimatedMinutes())
            .distanceKm(calculation.getDistanceKm())
            .routeId(calculation.getRouteId())
            .factors(Map.of(
                "traffic", calculation.getTrafficFactors(),
                "historical", calculation.getHistoricalFactors(),
                "depot", calculation.getDepotConstraints(),
                "vehicle", calculation.getVehicleFactors()
            ))
            .build();
    }
    
    private ETA createFallbackETA(Parcel parcel) {
        return ETA.builder()
            .parcelId(parcel.getParcelId())
            .estimatedArrival(LocalDateTime.now().plusHours(2)) // Fallback: 2 hours
            .confidence(ETA.ETAConfidence.VERY_LOW)
            .depotId(parcel.getDepotId())
            .driverId(parcel.getDriverId())
            .vehicleId(parcel.getVehicleId())
            .calculatedAt(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .estimatedMinutes(120)
            .build();
    }
}
