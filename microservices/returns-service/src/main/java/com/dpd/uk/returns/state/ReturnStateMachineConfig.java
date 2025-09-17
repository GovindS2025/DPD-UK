package com.dpd.uk.returns.state;

import com.dpd.uk.returns.model.ReturnRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;

@Slf4j
@Configuration
@EnableStateMachine
public class ReturnStateMachineConfig extends StateMachineConfigurerAdapter<ReturnRequest.ReturnStatus, ReturnEvent> {
    
    @Override
    public void configure(StateMachineStateConfigurer<ReturnRequest.ReturnStatus, ReturnEvent> states) throws Exception {
        states
            .withStates()
            .initial(ReturnRequest.ReturnStatus.REQUESTED)
            .states(ReturnRequest.ReturnStatus.values())
            .end(ReturnRequest.ReturnStatus.COMPLETED)
            .end(ReturnRequest.ReturnStatus.REJECTED)
            .end(ReturnRequest.ReturnStatus.EXPIRED)
            .end(ReturnRequest.ReturnStatus.CANCELLED);
    }
    
    @Override
    public void configure(StateMachineTransitionConfigurer<ReturnRequest.ReturnStatus, ReturnEvent> transitions) throws Exception {
        transitions
            // From REQUESTED
            .withExternal()
                .source(ReturnRequest.ReturnStatus.REQUESTED)
                .target(ReturnRequest.ReturnStatus.PENDING_APPROVAL)
                .event(ReturnEvent.SUBMIT_FOR_APPROVAL)
                .action(context -> log.info("Return submitted for approval: {}", context.getStateMachine().getId()))
            .and()
            .withExternal()
                .source(ReturnRequest.ReturnStatus.REQUESTED)
                .target(ReturnRequest.ReturnStatus.REJECTED)
                .event(ReturnEvent.REJECT)
                .action(context -> log.info("Return rejected: {}", context.getStateMachine().getId()))
            .and()
            .withExternal()
                .source(ReturnRequest.ReturnStatus.REQUESTED)
                .target(ReturnRequest.ReturnStatus.CANCELLED)
                .event(ReturnEvent.CANCEL)
                .action(context -> log.info("Return cancelled: {}", context.getStateMachine().getId()))
            
            // From PENDING_APPROVAL
            .withExternal()
                .source(ReturnRequest.ReturnStatus.PENDING_APPROVAL)
                .target(ReturnRequest.ReturnStatus.APPROVED)
                .event(ReturnEvent.APPROVE)
                .action(context -> log.info("Return approved: {}", context.getStateMachine().getId()))
            .and()
            .withExternal()
                .source(ReturnRequest.ReturnStatus.PENDING_APPROVAL)
                .target(ReturnRequest.ReturnStatus.REJECTED)
                .event(ReturnEvent.REJECT)
                .action(context -> log.info("Return rejected from pending: {}", context.getStateMachine().getId()))
            .and()
            .withExternal()
                .source(ReturnRequest.ReturnStatus.PENDING_APPROVAL)
                .target(ReturnRequest.ReturnStatus.EXPIRED)
                .event(ReturnEvent.EXPIRE)
                .action(context -> log.info("Return expired: {}", context.getStateMachine().getId()))
            
            // From APPROVED
            .withExternal()
                .source(ReturnRequest.ReturnStatus.APPROVED)
                .target(ReturnRequest.ReturnStatus.PICKUP_SCHEDULED)
                .event(ReturnEvent.SCHEDULE_PICKUP)
                .action(context -> log.info("Pickup scheduled: {}", context.getStateMachine().getId()))
            .and()
            .withExternal()
                .source(ReturnRequest.ReturnStatus.APPROVED)
                .target(ReturnRequest.ReturnStatus.CANCELLED)
                .event(ReturnEvent.CANCEL)
                .action(context -> log.info("Return cancelled after approval: {}", context.getStateMachine().getId()))
            
            // From PICKUP_SCHEDULED
            .withExternal()
                .source(ReturnRequest.ReturnStatus.PICKUP_SCHEDULED)
                .target(ReturnRequest.ReturnStatus.PICKED_UP)
                .event(ReturnEvent.PICKUP_COMPLETED)
                .action(context -> log.info("Pickup completed: {}", context.getStateMachine().getId()))
            .and()
            .withExternal()
                .source(ReturnRequest.ReturnStatus.PICKUP_SCHEDULED)
                .target(ReturnRequest.ReturnStatus.IN_TRANSIT)
                .event(ReturnEvent.START_TRANSPORT)
                .action(context -> log.info("Transport started: {}", context.getStateMachine().getId()))
            .and()
            .withExternal()
                .source(ReturnRequest.ReturnStatus.PICKUP_SCHEDULED)
                .target(ReturnRequest.ReturnStatus.CANCELLED)
                .event(ReturnEvent.CANCEL)
                .action(context -> log.info("Return cancelled during pickup: {}", context.getStateMachine().getId()))
            
            // From PICKED_UP
            .withExternal()
                .source(ReturnRequest.ReturnStatus.PICKED_UP)
                .target(ReturnRequest.ReturnStatus.IN_TRANSIT)
                .event(ReturnEvent.START_TRANSPORT)
                .action(context -> log.info("Transport started from pickup: {}", context.getStateMachine().getId()))
            
            // From IN_TRANSIT
            .withExternal()
                .source(ReturnRequest.ReturnStatus.IN_TRANSIT)
                .target(ReturnRequest.ReturnStatus.PROCESSING)
                .event(ReturnEvent.ARRIVE_AT_DEPOT)
                .action(context -> log.info("Arrived at depot: {}", context.getStateMachine().getId()))
            
            // From PROCESSING
            .withExternal()
                .source(ReturnRequest.ReturnStatus.PROCESSING)
                .target(ReturnRequest.ReturnStatus.COMPLETED)
                .event(ReturnEvent.PROCESSING_COMPLETED)
                .action(context -> log.info("Processing completed: {}", context.getStateMachine().getId()))
            
            // TTL expiry transitions
            .withExternal()
                .source(ReturnRequest.ReturnStatus.REQUESTED)
                .target(ReturnRequest.ReturnStatus.EXPIRED)
                .event(ReturnEvent.EXPIRE)
                .action(context -> log.info("Return expired from requested: {}", context.getStateMachine().getId()))
            .and()
            .withExternal()
                .source(ReturnRequest.ReturnStatus.APPROVED)
                .target(ReturnRequest.ReturnStatus.EXPIRED)
                .event(ReturnEvent.EXPIRE)
                .action(context -> log.info("Return expired from approved: {}", context.getStateMachine().getId()));
    }
    
    @Bean
    public StateMachineListener<ReturnRequest.ReturnStatus, ReturnEvent> stateMachineListener() {
        return new StateMachineListenerAdapter<ReturnRequest.ReturnStatus, ReturnEvent>() {
            @Override
            public void stateChanged(State<ReturnRequest.ReturnStatus, ReturnEvent> from, State<ReturnRequest.ReturnStatus, ReturnEvent> to) {
                log.info("State changed from {} to {}", 
                    from != null ? from.getId() : "null", 
                    to != null ? to.getId() : "null");
            }
            
            @Override
            public void transition(Transition<ReturnRequest.ReturnStatus, ReturnEvent> transition) {
                log.info("Transition from {} to {} on event {}", 
                    transition.getSource().getId(),
                    transition.getTarget().getId(),
                    transition.getTrigger().getEvent());
            }
        };
    }
    
    public enum ReturnEvent {
        SUBMIT_FOR_APPROVAL,
        APPROVE,
        REJECT,
        CANCEL,
        SCHEDULE_PICKUP,
        PICKUP_COMPLETED,
        START_TRANSPORT,
        ARRIVE_AT_DEPOT,
        PROCESSING_COMPLETED,
        EXPIRE
    }
}
