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
public class DepotConstraintsService {
    
    @Cacheable(value = "depot-constraints", key = "#depotId")
    public Map<String, Object> getDepotConstraints(String depotId) {
        try {
            log.debug("Fetching depot constraints for depot: {}", depotId);
            
            // In a real implementation, this would query depot management systems
            // For now, returning mock data based on depot characteristics
            return createDepotConstraints(depotId);
            
        } catch (Exception e) {
            log.warn("Failed to fetch depot constraints for depot: {}", depotId, e);
            return createFallbackDepotConstraints();
        }
    }
    
    private Map<String, Object> createDepotConstraints(String depotId) {
        Map<String, Object> constraints = new HashMap<>();
        
        // Mock depot-specific constraints
        switch (depotId) {
            case "LONDON_DEPOT" -> {
                constraints.put("multiplier", 1.1); // 10% delay due to urban constraints
                constraints.put("maxCapacity", 500);
                constraints.put("currentLoad", 350);
                constraints.put("driverAvailability", 0.85);
                constraints.put("vehicleAvailability", 0.90);
                constraints.put("processingTimeMinutes", 15);
                constraints.put("peakHours", new String[]{"08:00-10:00", "17:00-19:00"});
            }
            case "MANCHESTER_DEPOT" -> {
                constraints.put("multiplier", 1.05); // 5% delay
                constraints.put("maxCapacity", 300);
                constraints.put("currentLoad", 200);
                constraints.put("driverAvailability", 0.95);
                constraints.put("vehicleAvailability", 0.95);
                constraints.put("processingTimeMinutes", 10);
                constraints.put("peakHours", new String[]{"09:00-11:00", "16:00-18:00"});
            }
            case "BIRMINGHAM_DEPOT" -> {
                constraints.put("multiplier", 1.0); // No additional delay
                constraints.put("maxCapacity", 400);
                constraints.put("currentLoad", 250);
                constraints.put("driverAvailability", 0.90);
                constraints.put("vehicleAvailability", 0.88);
                constraints.put("processingTimeMinutes", 12);
                constraints.put("peakHours", new String[]{"08:30-10:30", "17:30-19:30"});
            }
            default -> {
                constraints.put("multiplier", 1.0);
                constraints.put("maxCapacity", 200);
                constraints.put("currentLoad", 100);
                constraints.put("driverAvailability", 0.80);
                constraints.put("vehicleAvailability", 0.85);
                constraints.put("processingTimeMinutes", 20);
                constraints.put("peakHours", new String[]{"09:00-11:00", "17:00-19:00"});
            }
        }
        
        // Add time-based constraints
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Check if current time is in peak hours
        boolean isPeakHour = isInPeakHours(hour, (String[]) constraints.get("peakHours"));
        if (isPeakHour) {
            double currentMultiplier = (Double) constraints.get("multiplier");
            constraints.put("multiplier", currentMultiplier * 1.2); // 20% additional delay during peak hours
            constraints.put("isPeakHour", true);
        } else {
            constraints.put("isPeakHour", false);
        }
        
        // Calculate capacity utilization
        int maxCapacity = (Integer) constraints.get("maxCapacity");
        int currentLoad = (Integer) constraints.get("currentLoad");
        double utilization = (double) currentLoad / maxCapacity;
        constraints.put("capacityUtilization", utilization);
        
        // Adjust multiplier based on capacity utilization
        if (utilization > 0.9) {
            double currentMultiplier = (Double) constraints.get("multiplier");
            constraints.put("multiplier", currentMultiplier * 1.15); // 15% additional delay when near capacity
        }
        
        constraints.put("lastUpdated", System.currentTimeMillis());
        return constraints;
    }
    
    private boolean isInPeakHours(int currentHour, String[] peakHours) {
        for (String peakHour : peakHours) {
            String[] times = peakHour.split("-");
            int startHour = Integer.parseInt(times[0].split(":")[0]);
            int endHour = Integer.parseInt(times[1].split(":")[0]);
            
            if (currentHour >= startHour && currentHour < endHour) {
                return true;
            }
        }
        return false;
    }
    
    private Map<String, Object> createFallbackDepotConstraints() {
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("multiplier", 1.0);
        constraints.put("maxCapacity", 200);
        constraints.put("currentLoad", 100);
        constraints.put("driverAvailability", 0.80);
        constraints.put("vehicleAvailability", 0.85);
        constraints.put("processingTimeMinutes", 20);
        constraints.put("peakHours", new String[]{"09:00-11:00", "17:00-19:00"});
        constraints.put("capacityUtilization", 0.5);
        constraints.put("isPeakHour", false);
        constraints.put("lastUpdated", System.currentTimeMillis());
        constraints.put("fallback", true);
        return constraints;
    }
}
