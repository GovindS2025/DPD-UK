package com.dpd.uk.returns.service;

import com.dpd.uk.returns.model.ReturnRequest;
import com.dpd.uk.returns.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TTLService {
    
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnOrchestrationService returnOrchestrationService;
    
    @Async
    public void startTTLMonitoring(ReturnRequest returnRequest) {
        log.info("Starting TTL monitoring for return: {}", returnRequest.getReturnId());
        
        // In a real implementation, this would use a more sophisticated scheduling system
        // For now, we'll rely on the scheduled method to check for expired returns
    }
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void checkExpiredReturns() {
        log.debug("Checking for expired returns");
        
        try {
            List<ReturnRequest> expiredReturns = returnRequestRepository
                .findByTtlExpiryBeforeAndIsActiveTrue(LocalDateTime.now());
            
            for (ReturnRequest returnRequest : expiredReturns) {
                log.info("Processing expired return: {}", returnRequest.getReturnId());
                returnOrchestrationService.expireReturn(returnRequest.getReturnId());
            }
            
            if (!expiredReturns.isEmpty()) {
                log.info("Processed {} expired returns", expiredReturns.size());
            }
            
        } catch (Exception e) {
            log.error("Error checking expired returns", e);
        }
    }
    
    @Scheduled(fixedRate = 600000) // Run every 10 minutes
    public void checkExpiringReturns() {
        log.debug("Checking for returns expiring soon");
        
        try {
            // Check for returns expiring in the next 2 hours
            List<ReturnRequest> expiringReturns = returnRequestRepository
                .findByTtlExpiryBeforeAndIsActiveTrue(LocalDateTime.now().plusHours(2));
            
            for (ReturnRequest returnRequest : expiringReturns) {
                if (returnRequest.getTtlExpiry().isAfter(LocalDateTime.now())) {
                    log.warn("Return {} is expiring soon at {}", 
                        returnRequest.getReturnId(), returnRequest.getTtlExpiry());
                    // In a real implementation, send warning notifications
                }
            }
            
        } catch (Exception e) {
            log.error("Error checking expiring returns", e);
        }
    }
    
    public void extendTTL(String returnId, int additionalHours) {
        returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId)
            .ifPresent(returnRequest -> {
                LocalDateTime newExpiry = returnRequest.getTtlExpiry().plusHours(additionalHours);
                returnRequest.setTtlExpiry(newExpiry);
                returnRequest.setLastUpdated(LocalDateTime.now());
                returnRequestRepository.save(returnRequest);
                
                log.info("Extended TTL for return {} by {} hours to {}", 
                    returnId, additionalHours, newExpiry);
            });
    }
    
    public void resetTTL(String returnId, int hours) {
        returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId)
            .ifPresent(returnRequest -> {
                LocalDateTime newExpiry = LocalDateTime.now().plusHours(hours);
                returnRequest.setTtlExpiry(newExpiry);
                returnRequest.setLastUpdated(LocalDateTime.now());
                returnRequestRepository.save(returnRequest);
                
                log.info("Reset TTL for return {} to {} hours from now", returnId, hours);
            });
    }
}
