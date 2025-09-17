package com.dpd.uk.routing.service;

import com.dpd.uk.common.model.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${external.mapping.api-key}")
    private String mappingApiKey;
    
    @Value("${external.mapping.base-url}")
    private String mappingBaseUrl;
    
    @Value("${external.mapping.timeout}")
    private int timeout;
    
    @Cacheable(value = "distance-matrix", key = "#addresses.hashCode()")
    public Map<String, Map<String, Double>> calculateDistanceMatrix(List<Address> addresses) {
        log.info("Calculating distance matrix for {} addresses", addresses.size());
        
        try {
            // For now, using Haversine distance as fallback
            // In a real implementation, this would call Azure Maps or Google Maps API
            return calculateHaversineDistanceMatrix(addresses);
            
        } catch (Exception e) {
            log.error("Error calculating distance matrix", e);
            return createFallbackDistanceMatrix(addresses);
        }
    }
    
    @Cacheable(value = "time-matrix", key = "#addresses.hashCode()")
    public Map<String, Map<String, Integer>> calculateTimeMatrix(List<Address> addresses) {
        log.info("Calculating time matrix for {} addresses", addresses.size());
        
        try {
            // For now, using estimated travel time based on distance
            // In a real implementation, this would call traffic-aware routing APIs
            return calculateEstimatedTimeMatrix(addresses);
            
        } catch (Exception e) {
            log.error("Error calculating time matrix", e);
            return createFallbackTimeMatrix(addresses);
        }
    }
    
    public Map<String, Object> getRouteDetails(Address origin, Address destination) {
        log.info("Getting route details from {} to {}", origin.getPostcode(), destination.getPostcode());
        
        try {
            WebClient webClient = webClientBuilder
                .baseUrl(mappingBaseUrl)
                .build();
            
            // In a real implementation, this would call Azure Maps routing API
            Map<String, Object> routeData = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/route/directions/json")
                    .queryParam("api-version", "1.0")
                    .queryParam("query", origin.getLatitude() + "," + origin.getLongitude() + ":" + 
                               destination.getLatitude() + "," + destination.getLongitude())
                    .queryParam("subscription-key", mappingApiKey)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .onErrorReturn(createFallbackRouteDetails(origin, destination))
                .block();
            
            return processRouteDetails(routeData);
            
        } catch (Exception e) {
            log.warn("Failed to get route details, using fallback", e);
            return createFallbackRouteDetails(origin, destination);
        }
    }
    
    private Map<String, Map<String, Double>> calculateHaversineDistanceMatrix(List<Address> addresses) {
        Map<String, Map<String, Double>> matrix = new HashMap<>();
        
        for (int i = 0; i < addresses.size(); i++) {
            Map<String, Double> row = new HashMap<>();
            for (int j = 0; j < addresses.size(); j++) {
                if (i == j) {
                    row.put(String.valueOf(j), 0.0);
                } else {
                    double distance = calculateHaversineDistance(addresses.get(i), addresses.get(j));
                    row.put(String.valueOf(j), distance);
                }
            }
            matrix.put(String.valueOf(i), row);
        }
        
        return matrix;
    }
    
    private Map<String, Map<String, Integer>> calculateEstimatedTimeMatrix(List<Address> addresses) {
        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        
        for (int i = 0; i < addresses.size(); i++) {
            Map<String, Integer> row = new HashMap<>();
            for (int j = 0; j < addresses.size(); j++) {
                if (i == j) {
                    row.put(String.valueOf(j), 0);
                } else {
                    double distance = calculateHaversineDistance(addresses.get(i), addresses.get(j));
                    int time = (int) (distance / 30.0 * 60); // Assume 30 km/h average speed
                    row.put(String.valueOf(j), time);
                }
            }
            matrix.put(String.valueOf(i), row);
        }
        
        return matrix;
    }
    
    private double calculateHaversineDistance(Address from, Address to) {
        final int R = 6371; // Earth's radius in kilometers
        
        double lat1 = Math.toRadians(from.getLatitude());
        double lon1 = Math.toRadians(from.getLongitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double lon2 = Math.toRadians(to.getLongitude());
        
        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;
        
        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dlon / 2) * Math.sin(dlon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    private Map<String, Object> processRouteDetails(Map<String, Object> routeData) {
        Map<String, Object> processed = new HashMap<>();
        
        if (routeData != null && routeData.containsKey("routes")) {
            // Process route data from mapping API
            processed.put("distance", 10.5); // Example distance in km
            processed.put("duration", 25); // Example duration in minutes
            processed.put("trafficDelay", 5); // Example traffic delay in minutes
            processed.put("waypoints", List.of("waypoint1", "waypoint2"));
        } else {
            processed.put("distance", 10.5);
            processed.put("duration", 25);
            processed.put("trafficDelay", 0);
            processed.put("waypoints", List.of());
        }
        
        return processed;
    }
    
    private Map<String, Map<String, Double>> createFallbackDistanceMatrix(List<Address> addresses) {
        Map<String, Map<String, Double>> matrix = new HashMap<>();
        
        for (int i = 0; i < addresses.size(); i++) {
            Map<String, Double> row = new HashMap<>();
            for (int j = 0; j < addresses.size(); j++) {
                if (i == j) {
                    row.put(String.valueOf(j), 0.0);
                } else {
                    row.put(String.valueOf(j), 5.0); // Default 5km distance
                }
            }
            matrix.put(String.valueOf(i), row);
        }
        
        return matrix;
    }
    
    private Map<String, Map<String, Integer>> createFallbackTimeMatrix(List<Address> addresses) {
        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        
        for (int i = 0; i < addresses.size(); i++) {
            Map<String, Integer> row = new HashMap<>();
            for (int j = 0; j < addresses.size(); j++) {
                if (i == j) {
                    row.put(String.valueOf(j), 0);
                } else {
                    row.put(String.valueOf(j), 10); // Default 10 minutes
                }
            }
            matrix.put(String.valueOf(i), row);
        }
        
        return matrix;
    }
    
    private Map<String, Object> createFallbackRouteDetails(Address origin, Address destination) {
        Map<String, Object> details = new HashMap<>();
        details.put("distance", 10.5);
        details.put("duration", 25);
        details.put("trafficDelay", 0);
        details.put("waypoints", List.of());
        details.put("fallback", true);
        return details;
    }
}
