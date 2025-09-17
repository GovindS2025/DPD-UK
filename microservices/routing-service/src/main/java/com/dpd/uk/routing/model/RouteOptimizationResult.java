package com.dpd.uk.routing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteOptimizationResult {
    
    private List<Integer> optimizedSequence;
    private Double totalDistance;
    private Integer totalTime;
    private Long optimizationTime;
    private Integer iterations;
    private Double fitness;
    private String algorithm;
    
    private Map<String, Object> metrics;
    private Map<String, Object> constraints;
    private List<String> warnings;
    private List<String> errors;
    
    private Boolean isOptimal;
    private Double optimalityGap;
    private String status; // SUCCESS, PARTIAL, FAILED
    
    // Performance metrics
    private Double averageSpeed;
    private Integer totalStops;
    private Double utilizationRate;
    private Double costEstimate;
    
    // Route quality indicators
    private Double routeEfficiency;
    private Double timeWindowCompliance;
    private Double capacityUtilization;
    private Double fuelEfficiency;
}
