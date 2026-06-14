package com.scheduler.controller;

import com.scheduler.dto.ChatMessageResponse;
import com.scheduler.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket controller for real-time chat messaging using STOMP.
 * Handles incoming chat messages and broadcasts AI responses in real-time.
 */
@Slf4j
@Controller
public class WebSocketChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Handles incoming chat messages via WebSocket
     * Messages sent to /app/chat are routed here
     * Responses are sent to /topic/chat/{conversationId}
     */
    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    public Map<String, Object> handleChatMessage(Map<String, Object> message) {
        try {
            Long userId = ((Number) message.get("userId")).longValue();
            Long conversationId = ((Number) message.get("conversationId")).longValue();
            String content = (String) message.get("content");
            String context = (String) message.getOrDefault("context", "general");

            log.info("Received WebSocket message in conversation: {} from user: {}", conversationId, userId);

            // Send message to ChatService and get AI response
            ChatMessageResponse aiResponse = chatService.sendMessage(userId, conversationId, content, context);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("id", aiResponse.getId());
            response.put("conversationId", aiResponse.getConversationId());
            response.put("userId", aiResponse.getUserId());
            response.put("content", aiResponse.getContent());
            response.put("role", aiResponse.getRole());
            response.put("createdAt", aiResponse.getCreatedAt());
            response.put("status", "success");

            log.info("Sending AI response via WebSocket: {}", aiResponse.getId());
            return response;

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error processing message: " + e.getMessage());
            return error;
        }
    }

    /**
     * Alternative endpoint for sending typed chat messages
     * Provides more structured message handling
     */
    @MessageMapping("/chat/send")
    @SendTo("/topic/chat/messages")
    public Map<String, Object> sendTypedMessage(ChatWebSocketMessage message) {
        try {
            log.info("Received typed WebSocket message in conversation: {}", message.getConversationId());

            // Send message to ChatService
            ChatMessageResponse aiResponse = chatService.sendMessage(
                    message.getUserId(),
                    message.getConversationId(),
                    message.getContent(),
                    message.getContext()
            );

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("type", "message");
            response.put("id", aiResponse.getId());
            response.put("conversationId", aiResponse.getConversationId());
            response.put("userId", aiResponse.getUserId());
            response.put("content", aiResponse.getContent());
            response.put("role", aiResponse.getRole());
            response.put("createdAt", aiResponse.getCreatedAt());
            response.put("status", "success");

            return response;

        } catch (Exception e) {
            log.error("Error handling typed WebSocket message", e);
            Map<String, Object> error = new HashMap<>();
            error.put("type", "error");
            error.put("status", "error");
            error.put("message", "Error: " + e.getMessage());
            return error;
        }
    }

    /**
     * Inner class for structured WebSocket message handling
     */
    public static class ChatWebSocketMessage {
        private Long userId;
        private Long conversationId;
        private String content;
        private String context;

        public ChatWebSocketMessage() {}

        public ChatWebSocketMessage(Long userId, Long conversationId, String content, String context) {
            this.userId = userId;
            this.conversationId = conversationId;
            this.content = content;
            this.context = context;
        }

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }
}

