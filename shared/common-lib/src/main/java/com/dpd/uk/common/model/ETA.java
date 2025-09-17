package com.dpd.uk.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ETA {
    
    @NotBlank
    private String parcelId;
    
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedArrival;
    
    @NotNull
    private ETAConfidence confidence;
    
    private String depotId;
    private String driverId;
    private String vehicleId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime calculatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;
    
    private Integer estimatedMinutes;
    private Double distanceKm;
    private String routeId;
    
    private Map<String, Object> factors;
    
    public enum ETAConfidence {
        HIGH(0.9),
        MEDIUM(0.7),
        LOW(0.5),
        VERY_LOW(0.3);
        
        private final double threshold;
        
        ETAConfidence(double threshold) {
            this.threshold = threshold;
        }
        
        public double getThreshold() {
            return threshold;
        }
    }
}
