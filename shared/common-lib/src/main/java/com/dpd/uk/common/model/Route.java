package com.dpd.uk.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    
    @NotBlank
    private String routeId;
    
    @NotBlank
    private String depotId;
    
    @NotBlank
    private String driverId;
    
    @NotBlank
    private String vehicleId;
    
    @NotNull
    private RouteStatus status;
    
    private List<RouteStop> stops;
    private List<RouteSegment> segments;
    
    private Double totalDistanceKm;
    private Integer estimatedDurationMinutes;
    private Integer actualDurationMinutes;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime plannedStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime plannedEndTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualEndTime;
    
    private String optimizationAlgorithm;
    private Map<String, Object> metadata;
    
    public enum RouteStatus {
        PLANNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        OPTIMIZING
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStop {
        private String stopId;
        private String parcelId;
        private Address address;
        private StopType type;
        private Integer sequence;
        private Integer estimatedArrivalMinutes;
        private Integer actualArrivalMinutes;
        private StopStatus status;
        
        public enum StopType {
            PICKUP,
            DELIVERY,
            DEPOT
        }
        
        public enum StopStatus {
            PENDING,
            IN_PROGRESS,
            COMPLETED,
            SKIPPED,
            FAILED
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteSegment {
        private String segmentId;
        private Address from;
        private Address to;
        private Double distanceKm;
        private Integer estimatedDurationMinutes;
        private Integer actualDurationMinutes;
        private TrafficCondition trafficCondition;
        private String roadType;
        
        public enum TrafficCondition {
            LIGHT,
            MODERATE,
            HEAVY,
            CONGESTED,
            BLOCKED
        }
    }
}
