package com.dpd.uk.returns.model;

import com.dpd.uk.common.model.Address;
import com.dpd.uk.common.model.Parcel;
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
@Document(collection = "return_requests")
public class ReturnRequest {
    
    @Id
    private String id;
    
    private String returnId;
    private String parcelId;
    private String trackingNumber;
    private String customerId;
    private String reason;
    private String description;
    
    private Address pickupAddress;
    private Address returnAddress;
    
    private ReturnStatus status;
    private ReturnType type;
    private ReturnPriority priority;
    
    private Double refundAmount;
    private String refundMethod;
    private String approvalCode;
    
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    
    private LocalDateTime ttlExpiry;
    private LocalDateTime lastUpdated;
    
    private String assignedDriverId;
    private String assignedVehicleId;
    private String depotId;
    
    private Map<String, Object> metadata;
    private Map<String, Object> customerInfo;
    private Map<String, Object> productInfo;
    
    private Boolean isActive;
    private String stateMachineId;
    
    public enum ReturnStatus {
        REQUESTED,
        PENDING_APPROVAL,
        APPROVED,
        PICKUP_SCHEDULED,
        PICKED_UP,
        IN_TRANSIT,
        PROCESSING,
        COMPLETED,
        REJECTED,
        EXPIRED,
        CANCELLED
    }
    
    public enum ReturnType {
        CUSTOMER_INITIATED,
        MERCHANT_INITIATED,
        DAMAGED_GOODS,
        WRONG_ITEM,
        NOT_AS_DESCRIBED,
        DEFECTIVE,
        UNWANTED
    }
    
    public enum ReturnPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}
