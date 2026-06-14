package com.scheduler.controller;

import com.scheduler.dto.ChatConversationResponse;
import com.scheduler.dto.ChatMessageRequest;
import com.scheduler.dto.ChatMessageResponse;
import com.scheduler.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST Controller for AI chat functionality.
 * Handles chat conversations, message history, and real-time messaging.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Creates a new chat conversation
     * POST /api/chat/conversations
     */
    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(
            @RequestParam Long userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String context) {
        try {
            log.info("Creating new conversation for user: {}", userId);
            ChatConversationResponse response = chatService.createConversation(userId, title, context);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating conversation", e);
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Failed to create conversation",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Gets all conversations for a user
     * GET /api/chat/conversations?userId=1
     */
    @GetMapping("/conversations")
    public ResponseEntity<?> getUserConversations(
            @RequestParam Long userId) {
        try {
            log.info("Retrieving conversations for user: {}", userId);
            List<ChatConversationResponse> conversations = chatService.getUserConversations(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("Error retrieving conversations", e);
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Failed to retrieve conversations",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Gets a specific conversation
     * GET /api/chat/conversations/{conversationId}
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long conversationId) {
        try {
            log.info("Retrieving conversation: {}", conversationId);
            ChatConversationResponse response = chatService.getConversation(conversationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving conversation", e);
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Failed to retrieve conversation",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Sends a message and gets AI response
     * POST /api/chat/messages
     */
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(
            @RequestParam Long userId,
            @RequestBody ChatMessageRequest request) {
        try {
            log.info("Sending message in conversation: {} for user: {}", request.getConversationId(), userId);
            ChatMessageResponse response = chatService.sendMessage(
                    userId,
                    request.getConversationId(),
                    request.getContent(),
                    request.getContext()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Failed to send message",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Gets conversation history
     * GET /api/chat/conversations/{conversationId}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getConversationHistory(
            @PathVariable Long conversationId) {
        try {
            log.info("Retrieving message history for conversation: {}", conversationId);
            List<ChatMessageResponse> messages = chatService.getConversationHistory(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving conversation history", e);
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Failed to retrieve message history",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Archives a conversation
     * POST /api/chat/conversations/{conversationId}/archive
     */
    @PostMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<?> archiveConversation(
            @PathVariable Long conversationId) {
        try {
            log.info("Archiving conversation: {}", conversationId);
            chatService.archiveConversation(conversationId);
            return ResponseEntity.ok(java.util.Map.of("message", "Conversation archived successfully"));
        } catch (Exception e) {
            log.error("Error archiving conversation", e);
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Failed to archive conversation",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /api/chat/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat Service is running");
    }
}

