package com.dpd.uk.notification.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        sessions.put(session.getId(), session);
        
        // Send welcome message
        sendMessage(session, Map.of(
            "type", "connection_established",
            "message", "Connected to logistics platform notifications",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        sessions.remove(session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received message from {}: {}", session.getId(), message.getPayload());
        
        // Handle client messages (e.g., subscription requests)
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");
            
            switch (type) {
                case "subscribe" -> handleSubscription(session, payload);
                case "unsubscribe" -> handleUnsubscription(session, payload);
                case "ping" -> handlePing(session);
                default -> log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(session, "Invalid message format");
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session: {}", session.getId(), exception);
        sessions.remove(session.getId());
    }
    
    public void broadcastNotification(Map<String, Object> notification) {
        log.info("Broadcasting notification to {} sessions", sessions.size());
        
        sessions.values().forEach(session -> {
            try {
                sendMessage(session, notification);
            } catch (Exception e) {
                log.error("Error sending notification to session: {}", session.getId(), e);
                sessions.remove(session.getId());
            }
        });
    }
    
    public void sendNotificationToSession(String sessionId, Map<String, Object> notification) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, notification);
            } catch (Exception e) {
                log.error("Error sending notification to session: {}", sessionId, e);
            }
        }
    }
    
    private void handleSubscription(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String topic = (String) payload.get("topic");
        log.info("Session {} subscribed to topic: {}", session.getId(), topic);
        
        // Store subscription info in session attributes
        session.getAttributes().put("subscribed_topic", topic);
        
        sendMessage(session, Map.of(
            "type", "subscription_confirmed",
            "topic", topic,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    private void handleUnsubscription(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String topic = (String) payload.get("topic");
        log.info("Session {} unsubscribed from topic: {}", session.getId(), topic);
        
        session.getAttributes().remove("subscribed_topic");
        
        sendMessage(session, Map.of(
            "type", "unsubscription_confirmed",
            "topic", topic,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    private void handlePing(WebSocketSession session) throws IOException {
        sendMessage(session, Map.of(
            "type", "pong",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }
    
    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        sendMessage(session, Map.of(
            "type", "error",
            "message", errorMessage,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    public int getActiveConnections() {
        return sessions.size();
    }
}
