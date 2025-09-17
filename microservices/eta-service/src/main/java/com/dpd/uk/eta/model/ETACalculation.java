package com.dpd.uk.eta.model;

import com.dpd.uk.common.model.Address;
import com.dpd.uk.common.model.ETA;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "eta_calculations")
public class ETACalculation {
    
    @Id
    private String id;
    
    private String parcelId;
    private String routeId;
    private String depotId;
    private String driverId;
    private String vehicleId;
    
    private Address origin;
    private Address destination;
    private Address currentLocation;
    
    private ETA.ETAConfidence confidence;
    private LocalDateTime estimatedArrival;
    private Integer estimatedMinutes;
    private Double distanceKm;
    
    private Map<String, Object> trafficFactors;
    private Map<String, Object> historicalFactors;
    private Map<String, Object> depotConstraints;
    private Map<String, Object> vehicleFactors;
    
    private LocalDateTime calculatedAt;
    private LocalDateTime lastUpdated;
    private String calculationVersion;
    
    private Boolean isActive;
    private String status;
}
