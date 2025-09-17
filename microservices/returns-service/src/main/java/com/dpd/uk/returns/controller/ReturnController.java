package com.dpd.uk.returns.controller;

import com.dpd.uk.returns.model.ReturnRequest;
import com.dpd.uk.returns.service.ReturnOrchestrationService;
import com.dpd.uk.returns.service.TTLService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {
    
    private final ReturnOrchestrationService returnOrchestrationService;
    private final TTLService ttlService;
    
    @PostMapping("/initiate")
    public ResponseEntity<ReturnRequest> initiateReturn(@Valid @RequestBody ReturnRequest returnRequest) {
        log.info("Received return initiation request for parcel: {}", returnRequest.getParcelId());
        
        try {
            ReturnRequest initiatedReturn = returnOrchestrationService.initiateReturn(returnRequest);
            return ResponseEntity.ok(initiatedReturn);
        } catch (Exception e) {
            log.error("Error initiating return for parcel: {}", returnRequest.getParcelId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{returnId}/approve")
    public ResponseEntity<ReturnRequest> approveReturn(@PathVariable String returnId, 
                                                     @RequestParam String approvalCode) {
        log.info("Received return approval request for return: {}", returnId);
        
        try {
            ReturnRequest approvedReturn = returnOrchestrationService.approveReturn(returnId, approvalCode);
            return ResponseEntity.ok(approvedReturn);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error approving return: {}", returnId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{returnId}/reject")
    public ResponseEntity<ReturnRequest> rejectReturn(@PathVariable String returnId, 
                                                    @RequestParam String reason) {
        log.info("Received return rejection request for return: {}", returnId);
        
        try {
            ReturnRequest rejectedReturn = returnOrchestrationService.rejectReturn(returnId, reason);
            return ResponseEntity.ok(rejectedReturn);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error rejecting return: {}", returnId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{returnId}/schedule-pickup")
    public ResponseEntity<ReturnRequest> schedulePickup(@PathVariable String returnId, 
                                                      @RequestParam String driverId,
                                                      @RequestParam String vehicleId) {
        log.info("Received pickup scheduling request for return: {}", returnId);
        
        try {
            ReturnRequest scheduledReturn = returnOrchestrationService.schedulePickup(returnId, driverId, vehicleId);
            return ResponseEntity.ok(scheduledReturn);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error scheduling pickup for return: {}", returnId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{returnId}/complete-pickup")
    public ResponseEntity<ReturnRequest> completePickup(@PathVariable String returnId) {
        log.info("Received pickup completion request for return: {}", returnId);
        
        try {
            ReturnRequest completedReturn = returnOrchestrationService.completePickup(returnId);
            return ResponseEntity.ok(completedReturn);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error completing pickup for return: {}", returnId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{returnId}/complete-processing")
    public ResponseEntity<ReturnRequest> completeProcessing(@PathVariable String returnId) {
        log.info("Received processing completion request for return: {}", returnId);
        
        try {
            ReturnRequest processedReturn = returnOrchestrationService.completeProcessing(returnId);
            return ResponseEntity.ok(processedReturn);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error completing processing for return: {}", returnId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnRequest> getReturn(@PathVariable String returnId) {
        log.info("Received return request for return: {}", returnId);
        
        Optional<ReturnRequest> returnRequest = returnOrchestrationService.getReturn(returnId);
        return returnRequest.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReturnRequest>> getReturnsByStatus(@PathVariable ReturnRequest.ReturnStatus status) {
        log.info("Received returns request for status: {}", status);
        
        List<ReturnRequest> returns = returnOrchestrationService.getReturnsByStatus(status);
        return ResponseEntity.ok(returns);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ReturnRequest>> getReturnsByCustomer(@PathVariable String customerId) {
        log.info("Received returns request for customer: {}", customerId);
        
        List<ReturnRequest> returns = returnOrchestrationService.getReturnsByCustomer(customerId);
        return ResponseEntity.ok(returns);
    }
    
    @GetMapping("/expiring")
    public ResponseEntity<List<ReturnRequest>> getExpiringReturns() {
        log.info("Received expiring returns request");
        
        List<ReturnRequest> returns = returnOrchestrationService.getExpiringReturns();
        return ResponseEntity.ok(returns);
    }
    
    @PostMapping("/{returnId}/extend-ttl")
    public ResponseEntity<Void> extendTTL(@PathVariable String returnId, 
                                        @RequestParam int additionalHours) {
        log.info("Received TTL extension request for return: {} for {} hours", returnId, additionalHours);
        
        try {
            ttlService.extendTTL(returnId, additionalHours);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error extending TTL for return: {}", returnId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{returnId}/reset-ttl")
    public ResponseEntity<Void> resetTTL(@PathVariable String returnId, 
                                       @RequestParam int hours) {
        log.info("Received TTL reset request for return: {} to {} hours", returnId, hours);
        
        try {
            ttlService.resetTTL(returnId, hours);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error resetting TTL for return: {}", returnId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        return ResponseEntity.ok(java.util.Map.of(
            "status", "UP",
            "service", "returns-service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
