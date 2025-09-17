package com.dpd.uk.eta.controller;

import com.dpd.uk.common.model.ETA;
import com.dpd.uk.common.model.Parcel;
import com.dpd.uk.eta.service.ETACalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/eta")
@RequiredArgsConstructor
public class ETAController {
    
    private final ETACalculationService etaCalculationService;
    
    @PostMapping("/calculate")
    public ResponseEntity<ETA> calculateETA(@Valid @RequestBody Parcel parcel) {
        log.info("Received ETA calculation request for parcel: {}", parcel.getParcelId());
        
        try {
            ETA eta = etaCalculationService.calculateETA(parcel);
            return ResponseEntity.ok(eta);
        } catch (Exception e) {
            log.error("Error calculating ETA for parcel: {}", parcel.getParcelId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{parcelId}")
    public ResponseEntity<ETA> getETA(@PathVariable String parcelId) {
        log.info("Received ETA request for parcel: {}", parcelId);
        
        Optional<ETA> eta = etaCalculationService.getETA(parcelId);
        return eta.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/depot/{depotId}")
    public ResponseEntity<List<ETA>> getETAsByDepot(@PathVariable String depotId) {
        log.info("Received ETA request for depot: {}", depotId);
        
        List<ETA> etas = etaCalculationService.getETAsByDepot(depotId);
        return ResponseEntity.ok(etas);
    }
    
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<ETA>> getETAsByDriver(@PathVariable String driverId) {
        log.info("Received ETA request for driver: {}", driverId);
        
        List<ETA> etas = etaCalculationService.getETAsByDriver(driverId);
        return ResponseEntity.ok(etas);
    }
    
    @PutMapping("/{parcelId}/update")
    public ResponseEntity<Void> updateETA(@PathVariable String parcelId, 
                                        @RequestBody Map<String, Object> updates) {
        log.info("Received ETA update request for parcel: {}", parcelId);
        
        try {
            etaCalculationService.updateETA(parcelId, updates);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating ETA for parcel: {}", parcelId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "eta-service",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}
