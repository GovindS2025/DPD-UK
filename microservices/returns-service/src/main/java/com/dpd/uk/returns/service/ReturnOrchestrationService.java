package com.dpd.uk.returns.service;

import com.dpd.uk.returns.model.ReturnRequest;
import com.dpd.uk.returns.repository.ReturnRequestRepository;
import com.dpd.uk.returns.state.ReturnStateMachineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnOrchestrationService {
    
    private final ReturnRequestRepository returnRequestRepository;
    private final StateMachineFactory<ReturnRequest.ReturnStatus, ReturnStateMachineConfig.ReturnEvent> stateMachineFactory;
    private final ReturnNotificationService returnNotificationService;
    private final TTLService ttlService;
    
    @Transactional
    public ReturnRequest initiateReturn(ReturnRequest returnRequest) {
        log.info("Initiating return for parcel: {}", returnRequest.getParcelId());
        
        // Generate return ID
        returnRequest.setReturnId("RET_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
        returnRequest.setStatus(ReturnRequest.ReturnStatus.REQUESTED);
        returnRequest.setRequestedAt(LocalDateTime.now());
        returnRequest.setLastUpdated(LocalDateTime.now());
        returnRequest.setIsActive(true);
        
        // Set TTL
        LocalDateTime ttlExpiry = LocalDateTime.now().plusHours(24); // Default 24 hours
        returnRequest.setTtlExpiry(ttlExpiry);
        
        // Save return request
        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);
        
        // Start state machine
        StateMachine<ReturnRequest.ReturnStatus, ReturnStateMachineConfig.ReturnEvent> stateMachine = 
            stateMachineFactory.getStateMachine();
        stateMachine.start();
        
        // Send event to state machine
        Message<ReturnStateMachineConfig.ReturnEvent> message = MessageBuilder
            .withPayload(ReturnStateMachineConfig.ReturnEvent.SUBMIT_FOR_APPROVAL)
            .setHeader("returnId", savedRequest.getReturnId())
            .build();
        
        stateMachine.sendEvent(message);
        
        // Update status based on state machine
        savedRequest.setStatus(stateMachine.getState().getId());
        savedRequest.setStateMachineId(stateMachine.getId());
        
        // Start TTL monitoring
        ttlService.startTTLMonitoring(savedRequest);
        
        // Notify about return initiation
        returnNotificationService.notifyReturnInitiated(savedRequest);
        
        return returnRequestRepository.save(savedRequest);
    }
    
    @Transactional
    public ReturnRequest approveReturn(String returnId, String approvalCode) {
        log.info("Approving return: {}", returnId);
        
        return returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId)
            .map(returnRequest -> {
                // Update approval details
                returnRequest.setApprovalCode(approvalCode);
                returnRequest.setApprovedAt(LocalDateTime.now());
                returnRequest.setLastUpdated(LocalDateTime.now());
                
                // Send approval event to state machine
                StateMachine<ReturnRequest.ReturnStatus, ReturnStateMachineConfig.ReturnEvent> stateMachine = 
                    stateMachineFactory.getStateMachine();
                stateMachine.start();
                
                Message<ReturnStateMachineConfig.ReturnEvent> message = MessageBuilder
                    .withPayload(ReturnStateMachineConfig.ReturnEvent.APPROVE)
                    .setHeader("returnId", returnId)
                    .build();
                
                stateMachine.sendEvent(message);
                
                // Update status
                returnRequest.setStatus(stateMachine.getState().getId());
                
                // Notify about approval
                returnNotificationService.notifyReturnApproved(returnRequest);
                
                return returnRequestRepository.save(returnRequest);
            })
            .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));
    }
    
    @Transactional
    public ReturnRequest rejectReturn(String returnId, String reason) {
        log.info("Rejecting return: {} with reason: {}", returnId, reason);
        
        return returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId)
            .map(returnRequest -> {
                returnRequest.setDescription(returnRequest.getDescription() + " | Rejection reason: " + reason);
                returnRequest.setLastUpdated(LocalDateTime.now());
                
                // Send rejection event to state machine
                StateMachine<ReturnRequest.ReturnStatus, ReturnStateMachineConfig.ReturnEvent> stateMachine = 
                    stateMachineFactory.getStateMachine();
                stateMachine.start();
                
                Message<ReturnStateMachineConfig.ReturnEvent> message = MessageBuilder
                    .withPayload(ReturnStateMachineConfig.ReturnEvent.REJECT)
                    .setHeader("returnId", returnId)
                    .build();
                
                stateMachine.sendEvent(message);
                
                // Update status
                returnRequest.setStatus(stateMachine.getState().getId());
                
                // Notify about rejection
                returnNotificationService.notifyReturnRejected(returnRequest);
                
                return returnRequestRepository.save(returnRequest);
            })
            .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));
    }
    
    @Transactional
    public ReturnRequest schedulePickup(String returnId, String driverId, String vehicleId) {
        log.info("Scheduling pickup for return: {} with driver: {}", returnId, driverId);
        
        return returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId)
            .map(returnRequest -> {
                returnRequest.setAssignedDriverId(driverId);
                returnRequest.setAssignedVehicleId(vehicleId);
                returnRequest.setLastUpdated(LocalDateTime.now());
                
                // Send pickup scheduling event to state machine
                StateMachine<ReturnRequest.ReturnStatus, ReturnStateMachineConfig.ReturnEvent> stateMachine = 
                    stateMachineFactory.getStateMachine();
                stateMachine.start();
                
                Message<ReturnStateMachineConfig.ReturnEvent> message = MessageBuilder
                    .withPayload(ReturnStateMachineConfig.ReturnEvent.SCHEDULE_PICKUP)
                    .setHeader("returnId", returnId)
                    .build();
                
                stateMachine.sendEvent(message);
                
                // Update status
                returnRequest.setStatus(stateMachine.getState().getId());
                
                return returnRequestRepository.save(returnRequest);
            })
            .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));
    }
    
    @Transactional
    public ReturnRequest completePickup(String returnId) {
        log.info("Completing pickup for return: {}", returnId);
        
        return returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId)
            .map(returnRequest -> {
                returnRequest.setPickedUpAt(LocalDateTime.now());
                returnRequest.setLastUpdated(LocalDateTime.now());
                
                // Send pickup completion event to state machine
                StateMachine<ReturnRequest.ReturnStatus, ReturnStateMachineConfig.ReturnEvent> stateMachine = 
                    stateMachineFactory.getStateMachine();
                stateMachine.start();
                
                Message<ReturnStateMachineConfig.ReturnEvent> message = MessageBuilder
                    .withPayload(ReturnStateMachineConfig.ReturnEvent.PICKUP_COMPLETED)
                    .setHeader("returnId", returnId)
                    .build();
                
                stateMachine.sendEvent(message);
                
                // Update status
                returnRequest.setStatus(stateMachine.getState().getId());
                
                // Notify about pickup completion
                returnNotificationService.notifyReturnPickedUp(returnRequest);
                
                return returnRequestRepository.save(returnRequest);
            })
            .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));
    }
    
    @Transactional
    public ReturnRequest completeProcessing(String returnId) {
        log.info("Completing processing for return: {}", returnId);
        
        return returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId)
            .map(returnRequest -> {
                returnRequest.setProcessedAt(LocalDateTime.now());
                returnRequest.setCompletedAt(LocalDateTime.now());
                returnRequest.setLastUpdated(LocalDateTime.now());
                
                // Send processing completion event to state machine
                StateMachine<ReturnRequest.ReturnStatus, ReturnStateMachineConfig.ReturnEvent> stateMachine = 
                    stateMachineFactory.getStateMachine();
                stateMachine.start();
                
                Message<ReturnStateMachineConfig.ReturnEvent> message = MessageBuilder
                    .withPayload(ReturnStateMachineConfig.ReturnEvent.PROCESSING_COMPLETED)
                    .setHeader("returnId", returnId)
                    .build();
                
                stateMachine.sendEvent(message);
                
                // Update status
                returnRequest.setStatus(stateMachine.getState().getId());
                
                // Notify about processing completion
                returnNotificationService.notifyReturnProcessed(returnRequest);
                
                return returnRequestRepository.save(returnRequest);
            })
            .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));
    }
    
    public Optional<ReturnRequest> getReturn(String returnId) {
        return returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId);
    }
    
    public List<ReturnRequest> getReturnsByStatus(ReturnRequest.ReturnStatus status) {
        return returnRequestRepository.findByStatusAndIsActiveTrue(status);
    }
    
    public List<ReturnRequest> getReturnsByCustomer(String customerId) {
        return returnRequestRepository.findByCustomerIdAndIsActiveTrue(customerId);
    }
    
    public List<ReturnRequest> getExpiringReturns() {
        return returnRequestRepository.findByTtlExpiryBeforeAndIsActiveTrue(LocalDateTime.now().plusHours(2));
    }
    
    @Transactional
    public void expireReturn(String returnId) {
        returnRequestRepository.findByReturnIdAndIsActiveTrue(returnId)
            .ifPresent(returnRequest -> {
                returnRequest.setStatus(ReturnRequest.ReturnStatus.EXPIRED);
                returnRequest.setLastUpdated(LocalDateTime.now());
                returnRequestRepository.save(returnRequest);
                
                // Notify about expiration
                returnNotificationService.notifyReturnExpired(returnRequest);
            });
    }
}
