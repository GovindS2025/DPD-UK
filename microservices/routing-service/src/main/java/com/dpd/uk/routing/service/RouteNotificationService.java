package com.dpd.uk.routing.service;

import com.dpd.uk.common.model.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteNotificationService {
    
    private final StreamBridge streamBridge;
    
    public void notifyRouteOptimized(Route route) {
        try {
            log.info("Sending route optimization notification for route: {}", route.getRouteId());
            
            // Send to Kafka topic for real-time updates
            streamBridge.send("route-optimized-out", route);
            
            log.debug("Route optimization notification sent successfully for route: {}", route.getRouteId());
            
        } catch (Exception e) {
            log.error("Failed to send route optimization notification for route: {}", route.getRouteId(), e);
        }
    }
    
    public void notifyRouteUpdated(Route route) {
        try {
            log.info("Sending route update notification for route: {}", route.getRouteId());
            
            // Send to Kafka topic for real-time updates
            streamBridge.send("route-update-out", route);
            
            log.debug("Route update notification sent successfully for route: {}", route.getRouteId());
            
        } catch (Exception e) {
            log.error("Failed to send route update notification for route: {}", route.getRouteId(), e);
        }
    }
}
