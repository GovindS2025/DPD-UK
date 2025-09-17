package com.dpd.uk.returns.service;

import com.dpd.uk.returns.model.ReturnRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnNotificationService {
    
    private final StreamBridge streamBridge;
    
    public void notifyReturnInitiated(ReturnRequest returnRequest) {
        try {
            log.info("Sending return initiated notification for return: {}", returnRequest.getReturnId());
            
            streamBridge.send("return-initiated-out", returnRequest);
            
            log.debug("Return initiated notification sent successfully for return: {}", returnRequest.getReturnId());
            
        } catch (Exception e) {
            log.error("Failed to send return initiated notification for return: {}", returnRequest.getReturnId(), e);
        }
    }
    
    public void notifyReturnApproved(ReturnRequest returnRequest) {
        try {
            log.info("Sending return approved notification for return: {}", returnRequest.getReturnId());
            
            streamBridge.send("return-approved-out", returnRequest);
            
            log.debug("Return approved notification sent successfully for return: {}", returnRequest.getReturnId());
            
        } catch (Exception e) {
            log.error("Failed to send return approved notification for return: {}", returnRequest.getReturnId(), e);
        }
    }
    
    public void notifyReturnRejected(ReturnRequest returnRequest) {
        try {
            log.info("Sending return rejected notification for return: {}", returnRequest.getReturnId());
            
            // Send to appropriate topic
            streamBridge.send("return-rejected-out", returnRequest);
            
            log.debug("Return rejected notification sent successfully for return: {}", returnRequest.getReturnId());
            
        } catch (Exception e) {
            log.error("Failed to send return rejected notification for return: {}", returnRequest.getReturnId(), e);
        }
    }
    
    public void notifyReturnPickedUp(ReturnRequest returnRequest) {
        try {
            log.info("Sending return picked up notification for return: {}", returnRequest.getReturnId());
            
            streamBridge.send("return-picked-up-out", returnRequest);
            
            log.debug("Return picked up notification sent successfully for return: {}", returnRequest.getReturnId());
            
        } catch (Exception e) {
            log.error("Failed to send return picked up notification for return: {}", returnRequest.getReturnId(), e);
        }
    }
    
    public void notifyReturnProcessed(ReturnRequest returnRequest) {
        try {
            log.info("Sending return processed notification for return: {}", returnRequest.getReturnId());
            
            streamBridge.send("return-processed-out", returnRequest);
            
            log.debug("Return processed notification sent successfully for return: {}", returnRequest.getReturnId());
            
        } catch (Exception e) {
            log.error("Failed to send return processed notification for return: {}", returnRequest.getReturnId(), e);
        }
    }
    
    public void notifyReturnExpired(ReturnRequest returnRequest) {
        try {
            log.info("Sending return expired notification for return: {}", returnRequest.getReturnId());
            
            streamBridge.send("return-expired-out", returnRequest);
            
            log.debug("Return expired notification sent successfully for return: {}", returnRequest.getReturnId());
            
        } catch (Exception e) {
            log.error("Failed to send return expired notification for return: {}", returnRequest.getReturnId(), e);
        }
    }
}
