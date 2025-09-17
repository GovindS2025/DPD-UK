package com.dpd.uk.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
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
public class Parcel {
    
    @NotBlank
    private String parcelId;
    
    @NotBlank
    private String trackingNumber;
    
    @NotNull
    private Address origin;
    
    @NotNull
    private Address destination;
    
    @NotNull
    private ParcelStatus status;
    
    @NotNull
    private ParcelType type;
    
    private Double weight;
    private Double length;
    private Double width;
    private Double height;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String depotId;
    private String driverId;
    private String vehicleId;
    
    private Map<String, Object> metadata;
    
    public enum ParcelStatus {
        PICKED_UP,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        FAILED_DELIVERY,
        RETURNED,
        RETURN_INITIATED,
        RETURN_APPROVED,
        RETURN_PICKED_UP,
        RETURN_PROCESSED
    }
    
    public enum ParcelType {
        STANDARD,
        EXPRESS,
        SAME_DAY,
        NEXT_DAY,
        INTERNATIONAL
    }
}
