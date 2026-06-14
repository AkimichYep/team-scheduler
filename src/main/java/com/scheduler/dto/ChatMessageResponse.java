package com.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for chat message responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("conversation_id")
    private Long conversationId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("role")
    private String role; // USER or ASSISTANT

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("metadata")
    private String metadata;
}

