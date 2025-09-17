package com.dpd.uk.eta.service;

import com.dpd.uk.common.model.Parcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataService {
    
    private final MongoTemplate mongoTemplate;
    
    @Cacheable(value = "historical-data", key = "#parcel.origin.postcode + '_' + #parcel.destination.postcode + '_' + #parcel.type")
    public Map<String, Object> getHistoricalFactors(Parcel parcel) {
        try {
            log.debug("Fetching historical data for route from {} to {}", 
                parcel.getOrigin().getPostcode(), parcel.getDestination().getPostcode());
            
            // Query historical delivery data for similar routes
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("origin.postcode").is(parcel.getOrigin().getPostcode())
                    .and("destination.postcode").is(parcel.getDestination().getPostcode())
                    .and("type").is(parcel.getType())
                    .and("calculatedAt").gte(LocalDateTime.now().minusDays(30))),
                Aggregation.group()
                    .avg("estimatedMinutes").as("avgMinutes")
                    .min("estimatedMinutes").as("minMinutes")
                    .max("estimatedMinutes").as("maxMinutes")
                    .count().as("sampleSize")
            );
            
            AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "eta_calculations", Map.class);
            
            Map<String, Object> historicalData = results.getUniqueMappedResult();
            
            return processHistoricalData(historicalData, parcel);
            
        } catch (Exception e) {
            log.warn("Failed to fetch historical data for parcel: {}", parcel.getParcelId(), e);
            return createFallbackHistoricalData();
        }
    }
    
    private Map<String, Object> processHistoricalData(Map<String, Object> rawData, Parcel parcel) {
        Map<String, Object> factors = new HashMap<>();
        
        if (rawData != null && rawData.containsKey("avgMinutes")) {
            Double avgMinutes = (Double) rawData.get("avgMinutes");
            Integer sampleSize = (Integer) rawData.get("sampleSize");
            
            // Calculate multiplier based on historical performance
            double baseTime = calculateBaseTimeForParcel(parcel);
            double multiplier = avgMinutes / baseTime;
            
            factors.put("multiplier", Math.max(0.5, Math.min(2.0, multiplier))); // Clamp between 0.5x and 2.0x
            factors.put("avgMinutes", avgMinutes);
            factors.put("sampleSize", sampleSize);
            factors.put("confidence", calculateConfidence(sampleSize));
            factors.put("lastUpdated", System.currentTimeMillis());
        } else {
            factors.put("multiplier", 1.0);
            factors.put("avgMinutes", null);
            factors.put("sampleSize", 0);
            factors.put("confidence", "LOW");
            factors.put("lastUpdated", System.currentTimeMillis());
        }
        
        return factors;
    }
    
    private double calculateBaseTimeForParcel(Parcel parcel) {
        // Simplified base time calculation
        // In reality, this would use sophisticated routing algorithms
        return 60.0; // Default 1 hour
    }
    
    private String calculateConfidence(Integer sampleSize) {
        if (sampleSize == null || sampleSize < 5) return "LOW";
        if (sampleSize < 20) return "MEDIUM";
        return "HIGH";
    }
    
    private Map<String, Object> createFallbackHistoricalData() {
        Map<String, Object> factors = new HashMap<>();
        factors.put("multiplier", 1.0);
        factors.put("avgMinutes", null);
        factors.put("sampleSize", 0);
        factors.put("confidence", "LOW");
        factors.put("lastUpdated", System.currentTimeMillis());
        factors.put("fallback", true);
        return factors;
    }
}
