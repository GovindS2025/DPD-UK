package com.dpd.uk.eta.service;

import com.dpd.uk.common.model.ETA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ETANotificationService {
    
    private final StreamBridge streamBridge;
    
    public void notifyETAUpdate(ETA eta) {
        try {
            log.info("Sending ETA update notification for parcel: {}", eta.getParcelId());
            
            // Send to Kafka topic for real-time updates
            streamBridge.send("eta-calculated-out", eta);
            
            // In a real implementation, this would also:
            // 1. Send WebSocket notifications to connected clients
            // 2. Send push notifications to mobile apps
            // 3. Update real-time dashboards
            // 4. Send email/SMS notifications if configured
            
            log.debug("ETA update notification sent successfully for parcel: {}", eta.getParcelId());
            
        } catch (Exception e) {
            log.error("Failed to send ETA update notification for parcel: {}", eta.getParcelId(), e);
        }
    }
    
    public void notifyETAChange(ETA oldEta, ETA newEta) {
        try {
            log.info("Sending ETA change notification for parcel: {} ({} -> {})", 
                newEta.getParcelId(), 
                oldEta.getEstimatedArrival(), 
                newEta.getEstimatedArrival());
            
            // Send change notification
            ETAChangeNotification notification = ETAChangeNotification.builder()
                .parcelId(newEta.getParcelId())
                .oldEta(oldEta)
                .newEta(newEta)
                .changeReason("TRAFFIC_UPDATE")
                .timestamp(System.currentTimeMillis())
                .build();
            
            streamBridge.send("eta-change-out", notification);
            
            log.debug("ETA change notification sent successfully for parcel: {}", newEta.getParcelId());
            
        } catch (Exception e) {
            log.error("Failed to send ETA change notification for parcel: {}", newEta.getParcelId(), e);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ETAChangeNotification {
        private String parcelId;
        private ETA oldEta;
        private ETA newEta;
        private String changeReason;
        private Long timestamp;
    }
}
