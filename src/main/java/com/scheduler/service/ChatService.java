package com.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.dto.ChatConversationResponse;
import com.scheduler.dto.ChatMessageResponse;
import com.scheduler.model.ChatConversation;
import com.scheduler.model.ChatMessage;
import com.scheduler.model.User;
import com.scheduler.repository.ChatConversationRepository;
import com.scheduler.repository.ChatMessageRepository;
import com.scheduler.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing AI chat conversations with scheduling context.
 * Handles message persistence, conversation management, and AI responses.
 */
@Slf4j
@Service
public class ChatService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatConversationRepository chatConversationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Creates a new chat conversation
     */
    public ChatConversationResponse createConversation(Long userId, String title, String context) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ChatConversation conversation = ChatConversation.builder()
                    .user(user)
                    .title(title != null ? title : "New Conversation")
                    .description("")
                    .context(context != null ? context : "general")
                    .active(true)
                    .tokenCount(0L)
                    .build();

            conversation = chatConversationRepository.save(conversation);
            log.info("Created new conversation: {} for user: {}", conversation.getId(), userId);

            return mapToConversationResponse(conversation, 0);
        } catch (Exception e) {
            log.error("Error creating conversation for user: {}", userId, e);
            throw new RuntimeException("Failed to create conversation: " + e.getMessage());
        }
    }

    /**
     * Sends a message and gets AI response
     */
    public ChatMessageResponse sendMessage(Long userId, Long conversationId, String userMessage, String context) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ChatConversation conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            // Save user message
            ChatMessage userMsg = ChatMessage.builder()
                    .user(user)
                    .conversation(conversation)
                    .content(userMessage)
                    .role(ChatMessage.MessageRole.USER)
                    .build();
            userMsg = chatMessageRepository.save(userMsg);
            log.info("Saved user message: {} in conversation: {}", userMsg.getId(), conversationId);

            // Build context-aware prompt
            String enhancedPrompt = buildContextAwarePrompt(userId, userMessage, context);

            // Get AI response using ChatClient
            String aiResponse = chatClient.prompt()
                    .user(enhancedPrompt)
                    .call()
                    .content();

            // Save AI response
            ChatMessage assistantMsg = ChatMessage.builder()
                    .user(user)
                    .conversation(conversation)
                    .content(aiResponse)
                    .role(ChatMessage.MessageRole.ASSISTANT)
                    .metadata(buildMetadata(context))
                    .build();
            assistantMsg = chatMessageRepository.save(assistantMsg);
            log.info("Saved AI response: {} in conversation: {}", assistantMsg.getId(), conversationId);

            // Update conversation timestamp
            conversation.setUpdatedAt(java.time.LocalDateTime.now());
            chatConversationRepository.save(conversation);

            return mapToMessageResponse(assistantMsg);
        } catch (Exception e) {
            log.error("Error sending message in conversation: {}", conversationId, e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Retrieves conversation history
     */
    public List<ChatMessageResponse> getConversationHistory(Long conversationId) {
        try {
            List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            return messages.stream()
                    .map(this::mapToMessageResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving conversation history: {}", conversationId, e);
            throw new RuntimeException("Failed to retrieve conversation history: " + e.getMessage());
        }
    }

    /**
     * Retrieves all conversations for a user
     */
    public List<ChatConversationResponse> getUserConversations(Long userId) {
        try {
            List<ChatConversation> conversations = chatConversationRepository
                    .findByUserIdAndActiveOrderByUpdatedAtDesc(userId, true);

            return conversations.stream()
                    .map(conv -> {
                        long messageCount = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId()).size();
                        return mapToConversationResponse(conv, (int) messageCount);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving conversations for user: {}", userId, e);
            throw new RuntimeException("Failed to retrieve conversations: " + e.getMessage());
        }
    }

    /**
     * Gets a specific conversation
     */
    public ChatConversationResponse getConversation(Long conversationId) {
        try {
            ChatConversation conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            long messageCount = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).size();
            return mapToConversationResponse(conversation, (int) messageCount);
        } catch (Exception e) {
            log.error("Error retrieving conversation: {}", conversationId, e);
            throw new RuntimeException("Failed to retrieve conversation: " + e.getMessage());
        }
    }

    /**
     * Archives a conversation
     */
    public void archiveConversation(Long conversationId) {
        try {
            ChatConversation conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            conversation.setActive(false);
            chatConversationRepository.save(conversation);
            log.info("Archived conversation: {}", conversationId);
        } catch (Exception e) {
            log.error("Error archiving conversation: {}", conversationId, e);
            throw new RuntimeException("Failed to archive conversation: " + e.getMessage());
        }
    }

    /**
     * Builds a context-aware prompt that includes schedule information
     */
    private String buildContextAwarePrompt(Long userId, String userMessage, String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert team scheduling assistant. ");
        prompt.append("Help users with their scheduling questions, provide recommendations based on their data, ");
        prompt.append("and offer insights into workload distribution and team optimization.\n\n");

        // Add scheduling context if requested
        if (context != null && context.contains("schedule")) {
            try {
                // Get user's latest schedule summary
                long scheduleCount = scheduleService.getAllScheduleEntries().size();
                prompt.append("Current Schedule Context:\n");
                prompt.append("- User has ").append(scheduleCount).append(" schedule entries\n");
                prompt.append("- You should reference their schedule data when relevant\n\n");
            } catch (Exception e) {
                log.warn("Could not load schedule context", e);
            }
        }

        prompt.append("User's Question/Request:\n");
        prompt.append(userMessage).append("\n\n");
        prompt.append("Please provide a helpful, specific response. If asking about schedules, ");
        prompt.append("consider best practices for team scheduling and work-life balance.");

        return prompt.toString();
    }

    /**
     * Builds metadata string for storing with messages
     */
    private String buildMetadata(String context) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "context", context != null ? context : "general",
                    "timestamp", java.time.LocalDateTime.now().toString(),
                    "sourceType", "schedule-assistant"
            ));
        } catch (Exception e) {
            log.warn("Error building metadata", e);
            return "{}";
        }
    }

    /**
     * Maps ChatMessage to ChatMessageResponse DTO
     */
    private ChatMessageResponse mapToMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .userId(message.getUser().getId())
                .content(message.getContent())
                .role(message.getRole().toString())
                .createdAt(message.getCreatedAt())
                .metadata(message.getMetadata())
                .build();
    }

    /**
     * Maps ChatConversation to ChatConversationResponse DTO
     */
    private ChatConversationResponse mapToConversationResponse(ChatConversation conversation, int messageCount) {
        return ChatConversationResponse.builder()
                .id(conversation.getId())
                .userId(conversation.getUser().getId())
                .title(conversation.getTitle())
                .description(conversation.getDescription())
                .context(conversation.getContext())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .active(conversation.isActive())
                .messageCount(messageCount)
                .build();
    }
}

