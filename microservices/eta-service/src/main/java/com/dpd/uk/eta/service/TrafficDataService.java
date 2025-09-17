package com.dpd.uk.eta.service;

import com.dpd.uk.common.model.Parcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficDataService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${external.traffic.api-key}")
    private String trafficApiKey;
    
    @Value("${external.traffic.base-url}")
    private String trafficBaseUrl;
    
    @Value("${external.traffic.timeout}")
    private int timeout;
    
    @Cacheable(value = "traffic-data", key = "#parcel.origin.latitude + '_' + #parcel.origin.longitude + '_' + #parcel.destination.latitude + '_' + #parcel.destination.longitude")
    public Map<String, Object> getTrafficFactors(Parcel parcel) {
        try {
            log.debug("Fetching traffic data for route from {} to {}", 
                parcel.getOrigin().getPostcode(), parcel.getDestination().getPostcode());
            
            WebClient webClient = webClientBuilder
                .baseUrl(trafficBaseUrl)
                .build();
            
            // In a real implementation, this would call an actual traffic API
            // For now, returning mock data
            Map<String, Object> trafficData = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/traffic/route")
                    .queryParam("origin", parcel.getOrigin().getLatitude() + "," + parcel.getOrigin().getLongitude())
                    .queryParam("destination", parcel.getDestination().getLatitude() + "," + parcel.getDestination().getLongitude())
                    .queryParam("apiKey", trafficApiKey)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .onErrorReturn(createFallbackTrafficData())
                .block();
            
            return processTrafficData(trafficData);
            
        } catch (Exception e) {
            log.warn("Failed to fetch traffic data for parcel: {}", parcel.getParcelId(), e);
            return createFallbackTrafficData();
        }
    }
    
    private Map<String, Object> processTrafficData(Map<String, Object> rawData) {
        Map<String, Object> factors = new HashMap<>();
        
        if (rawData != null && rawData.containsKey("routes")) {
            // Process traffic data to extract relevant factors
            factors.put("multiplier", 1.2); // Example: 20% delay due to traffic
            factors.put("congestionLevel", "MODERATE");
            factors.put("incidents", 2);
            factors.put("lastUpdated", System.currentTimeMillis());
        } else {
            factors.put("multiplier", 1.0);
            factors.put("congestionLevel", "LIGHT");
            factors.put("incidents", 0);
            factors.put("lastUpdated", System.currentTimeMillis());
        }
        
        return factors;
    }
    
    private Map<String, Object> createFallbackTrafficData() {
        Map<String, Object> factors = new HashMap<>();
        factors.put("multiplier", 1.0);
        factors.put("congestionLevel", "UNKNOWN");
        factors.put("incidents", 0);
        factors.put("lastUpdated", System.currentTimeMillis());
        factors.put("fallback", true);
        return factors;
    }
}
