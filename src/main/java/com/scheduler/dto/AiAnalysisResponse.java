package com.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AI analysis responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisResponse {

    @JsonProperty("analysis_type")
    private String analysisType;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("content")
    private String content;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("error_message")
    private String errorMessage;
}

