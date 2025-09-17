package com.dpd.uk.routing.model;

import com.dpd.uk.common.model.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class RouteOptimizationRequest {
    
    @NotBlank
    private String depotId;
    
    @NotBlank
    private String driverId;
    
    @NotBlank
    private String vehicleId;
    
    @NotEmpty
    private List<Address> stops;
    
    @NotNull
    private String algorithm;
    
    private Integer maxDurationHours;
    private Double maxDistanceKm;
    private Integer maxStops;
    private Double vehicleCapacityWeight;
    private Double vehicleCapacityVolume;
    
    private Map<String, Object> constraints;
    private Map<String, Object> preferences;
    
    // Algorithm-specific parameters
    private Integer maxIterations;
    private Integer populationSize;
    private Double mutationRate;
    private Double crossoverRate;
    private Double coolingRate;
    private Integer tabuListSize;
    
    private String priority; // HIGH, MEDIUM, LOW
    private Boolean allowRerouting;
    private Boolean considerTraffic;
    private Boolean considerTimeWindows;
}
