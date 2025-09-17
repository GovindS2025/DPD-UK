package com.dpd.uk.eta.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTelematicsService {
    
    @Cacheable(value = "vehicle-telematics", key = "#vehicleId")
    public Map<String, Object> getVehicleFactors(String vehicleId) {
        try {
            log.debug("Fetching vehicle telematics for vehicle: {}", vehicleId);
            
            // In a real implementation, this would query vehicle telematics systems
            // For now, returning mock data based on vehicle characteristics
            return createVehicleFactors(vehicleId);
            
        } catch (Exception e) {
            log.warn("Failed to fetch vehicle telematics for vehicle: {}", vehicleId, e);
            return createFallbackVehicleFactors();
        }
    }
    
    private Map<String, Object> createVehicleFactors(String vehicleId) {
        Map<String, Object> factors = new HashMap<>();
        
        // Mock vehicle-specific factors
        if (vehicleId != null) {
            if (vehicleId.startsWith("VAN_")) {
                factors.put("multiplier", 1.0); // Vans are efficient for urban delivery
                factors.put("vehicleType", "VAN");
                factors.put("maxSpeed", 50); // Urban speed limit
                factors.put("fuelEfficiency", 0.8);
                factors.put("reliability", 0.95);
                factors.put("lastMaintenance", LocalDateTime.now().minusDays(7));
                factors.put("mileage", 50000);
            } else if (vehicleId.startsWith("TRUCK_")) {
                factors.put("multiplier", 1.1); // Trucks are slower but carry more
                factors.put("vehicleType", "TRUCK");
                factors.put("maxSpeed", 40); // Lower speed for larger vehicles
                factors.put("fuelEfficiency", 0.6);
                factors.put("reliability", 0.90);
                factors.put("lastMaintenance", LocalDateTime.now().minusDays(3));
                factors.put("mileage", 75000);
            } else if (vehicleId.startsWith("BIKE_")) {
                factors.put("multiplier", 0.9); // Bikes are fastest for short distances
                factors.put("vehicleType", "BIKE");
                factors.put("maxSpeed", 25); // Bike speed
                factors.put("fuelEfficiency", 1.0); // Electric bikes
                factors.put("reliability", 0.98);
                factors.put("lastMaintenance", LocalDateTime.now().minusDays(1));
                factors.put("mileage", 10000);
            } else {
                // Default vehicle
                factors.put("multiplier", 1.0);
                factors.put("vehicleType", "UNKNOWN");
                factors.put("maxSpeed", 45);
                factors.put("fuelEfficiency", 0.7);
                factors.put("reliability", 0.85);
                factors.put("lastMaintenance", LocalDateTime.now().minusDays(14));
                factors.put("mileage", 60000);
            }
        } else {
            factors.put("multiplier", 1.0);
            factors.put("vehicleType", "UNKNOWN");
            factors.put("maxSpeed", 45);
            factors.put("fuelEfficiency", 0.7);
            factors.put("reliability", 0.85);
            factors.put("lastMaintenance", LocalDateTime.now().minusDays(14));
            factors.put("mileage", 60000);
        }
        
        // Add real-time factors
        factors.put("currentLocation", "DEPOT");
        factors.put("batteryLevel", 85); // For electric vehicles
        factors.put("fuelLevel", 75); // For fuel vehicles
        factors.put("isInUse", false);
        factors.put("driverAssigned", false);
        
        // Calculate maintenance factor
        LocalDateTime lastMaintenance = (LocalDateTime) factors.get("lastMaintenance");
        long daysSinceMaintenance = java.time.Duration.between(lastMaintenance, LocalDateTime.now()).toDays();
        
        if (daysSinceMaintenance > 30) {
            double currentMultiplier = (Double) factors.get("multiplier");
            factors.put("multiplier", currentMultiplier * 1.1); // 10% delay for overdue maintenance
            factors.put("maintenanceOverdue", true);
        } else {
            factors.put("maintenanceOverdue", false);
        }
        
        // Calculate reliability factor
        double reliability = (Double) factors.get("reliability");
        if (reliability < 0.8) {
            double currentMultiplier = (Double) factors.get("multiplier");
            factors.put("multiplier", currentMultiplier * 1.2); // 20% delay for unreliable vehicles
        }
        
        factors.put("lastUpdated", System.currentTimeMillis());
        return factors;
    }
    
    private Map<String, Object> createFallbackVehicleFactors() {
        Map<String, Object> factors = new HashMap<>();
        factors.put("multiplier", 1.0);
        factors.put("vehicleType", "UNKNOWN");
        factors.put("maxSpeed", 45);
        factors.put("fuelEfficiency", 0.7);
        factors.put("reliability", 0.85);
        factors.put("lastMaintenance", LocalDateTime.now().minusDays(14));
        factors.put("mileage", 60000);
        factors.put("currentLocation", "UNKNOWN");
        factors.put("batteryLevel", 50);
        factors.put("fuelLevel", 50);
        factors.put("isInUse", false);
        factors.put("driverAssigned", false);
        factors.put("maintenanceOverdue", false);
        factors.put("lastUpdated", System.currentTimeMillis());
        factors.put("fallback", true);
        return factors;
    }
}
