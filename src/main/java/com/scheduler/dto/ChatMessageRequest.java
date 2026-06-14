package com.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for chat message requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {

    @JsonProperty("conversation_id")
    private Long conversationId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("context")
    private String context; // Optional context like "schedule-related", "team-discussion", etc.
}

