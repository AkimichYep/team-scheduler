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
     * Builds a context-aware prompt that includes schedule information and page content
     */
    private String buildContextAwarePrompt(Long userId, String userMessage, String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert team scheduling assistant for a company.\n");
        prompt.append("You help users with scheduling questions, provide specific recommendations, ");
        prompt.append("and offer insights into workload distribution and team optimization.\n");
        prompt.append("Always be helpful, specific, and reference actual data when available.\n\n");

        // Check if this is a schedule-specific question
        boolean isScheduleQuestion = isScheduleRelatedQuestion(userMessage);
        
        if (isScheduleQuestion) {
            prompt.append("=== SCHEDULE QUESTION DETECTED ===\n");
            prompt.append("User is asking about: schedule, shifts, who works, or similar.\n");
            try {
                // Get schedule count ONLY - don't expose raw objects with passwords
                java.util.List<?> allEntries = scheduleService.getAllScheduleEntries();
                if (allEntries != null && !allEntries.isEmpty()) {
                    prompt.append("System has ").append(allEntries.size()).append(" total schedule entries.\n");
                    //prompt.append("System has information about ").append(allEntries.subList(0, 10)).append(" total schedules for users.\n");
                    prompt.append("Schedule data is available in the system.\n");
                } else {
                    prompt.append("No schedule entries found in system.\n");
                }
            } catch (Exception e) {
                log.warn("Could not load schedule data", e);
                prompt.append("Could not load schedule statistics.\n");
            }
            prompt.append("Provide specific details if visible on page. If data is limited, suggest checking the scheduler page.\n\n");
        }

        // Parse and add page/schedule context if provided (SANITIZED)
        if (context != null && !context.isEmpty()) {
            String sanitizedContext = sanitizeContext(context);

            prompt.append("=== CURRENT PAGE CONTEXT ===\n");
            
            // Check if this is schedule data
            if (sanitizedContext.contains("Page: ") || sanitizedContext.contains("SCHEDULE DATA") || sanitizedContext.contains("Schedule")) {
                prompt.append("The user is viewing a SCHEDULE-related page.\n");
                
                // Extract and highlight schedule data
                if (sanitizedContext.contains("=== SCHEDULE DATA ===")) {
                    String[] parts = sanitizedContext.split("=== SCHEDULE DATA ===");
                    if (parts.length > 1) {
                        String scheduleData = parts[1].split("=== PAGE CONTENT ===")[0].trim();
                        prompt.append("Schedule Information Available:\n");
                        prompt.append(scheduleData).append("\n\n");
                    }
                } else {
                    // If schedule question but limited context
                    prompt.append("Limited schedule context available.\n");
                    prompt.append("Visible content:\n");
                    prompt.append(sanitizedContext.substring(0, Math.min(500, sanitizedContext.length()))).append("\n\n");
                }
            } else if (isScheduleQuestion) {
                // Schedule question but on non-schedule page
                prompt.append("User is asking about schedule but currently on a different page.\n");
                prompt.append("Visible page content:\n");
                prompt.append(sanitizedContext.substring(0, Math.min(300, sanitizedContext.length()))).append("\n\n");
            } else {
                // General context
                prompt.append(sanitizedContext.substring(0, Math.min(600, sanitizedContext.length()))).append("\n\n");
            }
        }

        // Add instructions
        prompt.append("=== INSTRUCTIONS ===\n");
        if (isScheduleQuestion) {
            prompt.append("1. PRIORITIZE giving specific names and times if visible on page\n");
            prompt.append("2. If on scheduler page: Reference the specific shifts and team members shown\n");
            prompt.append("3. If not on scheduler: Suggest viewing the Scheduler page for details\n");
            prompt.append("4. Use system statistics (call total count) but note that specific details need the scheduler page\n");
            prompt.append("5. Be direct and practical - give names, times, and roles when possible\n");
        } else {
            prompt.append("1. Reference page content when available\n");
            prompt.append("2. Be specific and practical\n");
            prompt.append("3. Use data from current page\n");
        }
        prompt.append("\n");

        prompt.append("=== USER QUESTION ===\n");
        prompt.append(userMessage).append("\n\n");
        
        if (isScheduleQuestion) {
            prompt.append("Respond with specific names, times, and details if available. ");
            prompt.append("If data is limited, acknowledge what you can see and suggest checking the scheduler for complete information.");
        } else {
            prompt.append("Please provide a specific, helpful response based on the context above.");
        }

        return prompt.toString();
    }

    /**
     * Sanitizes context to remove sensitive information before sharing with AI
     */
    private String sanitizeContext(String context) {
        if (context == null || context.isEmpty()) {
            return "";
        }

        String sanitized = context;

        // Remove sensitive patterns
        // Remove any password fields
        sanitized = sanitized.replaceAll("(?i)(password|pwd|pass)\\s*[=:]\\s*[\\w\\W]*?(?=\\n|\\||$)", "[REDACTED]");

        // Remove API keys
        sanitized = sanitized.replaceAll("(?i)(api[_-]?key|apikey|token)\\s*[=:]\\s*[\\w\\W]*?(?=\\n|\\||$)", "[REDACTED]");

        // Remove email addresses (optional - depending on privacy policy)
        // sanitized = sanitized.replaceAll("[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}", "[USER_EMAIL]");

        // Remove any sequences that look like tokens
        sanitized = sanitized.replaceAll("(?i)(bearer|basic)\\s+[\\w\\-\\.]+", "[AUTH_TOKEN]");

        // Limit context length to prevent excessive data
        int maxLength = 3000;
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength) + "\n[... context truncated ...]";
        }

        return sanitized;
    }

    /**
     * Detects if user message is asking about schedule/shifts
     */
    private boolean isScheduleRelatedQuestion(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("tomorrow") || 
               lower.contains("today") ||
               lower.contains("schedule") || 
               lower.contains("shift") || 
               lower.contains("work") ||
               lower.contains("who works") ||
               lower.contains("team") ||
               lower.contains("hour") ||
               lower.contains("employee") ||
               lower.contains("staff") ||
               lower.contains("coverage");
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

