package com.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Service for analyzing schedules using Spring AI with GROQ.
 * Uses an AI agent to provide insights and recommendations for schedule optimization.
 */
@Slf4j
@Service
public class ScheduleAnalyzerService {

    private final ChatClient chatClient;
    private final ScheduleService scheduleService;
    private final ObjectMapper objectMapper;

    public ScheduleAnalyzerService(ChatClient.Builder chatClientBuilder,
                                   ScheduleService scheduleService) {
        this.chatClient = chatClientBuilder
                .build();
        this.scheduleService = scheduleService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyzes the current schedule and provides recommendations.
     *
     * @return Analysis results with insights and recommendations
     */
    public String analyzeSchedule() {
        try {
            // Get schedule statistics
            long totalSchedules = scheduleService.getAllScheduleEntries().size();

            String prompt = String.format(
                """
                You are an expert team scheduler assistant. Analyze the following team schedule data and provide:
                1. Current schedule overview
                2. Potential issues or conflicts
                3. Optimization recommendations
                4. Team workload distribution insights
                
                Current Statistics:
                - Total schedule entries: %d
                
                Based on this data, provide constructive analysis and actionable recommendations 
                to improve team scheduling efficiency and employee satisfaction.
                """, totalSchedules
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Schedule analysis completed successfully");
            return response;
        } catch (Exception e) {
            log.error("Error analyzing schedule", e);
            throw new RuntimeException("Failed to analyze schedule: " + e.getMessage());
        }
    }

    /**
     * Analyzes a specific user's schedule.
     *
     * @param userId The ID of the user to analyze
     * @return Personalized analysis for the user
     */
    public String analyzeUserSchedule(Long userId) {
        try {
            String prompt = String.format(
                """
                You are an expert team scheduler assistant. Analyze the schedule for user ID %d and provide:
                1. Summary of their shift patterns
                2. Workload analysis
                3. Recommendations for better work-life balance
                4. Potential improvements to their schedule
                
                Provide a personalized analysis with actionable recommendations.
                """, userId
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("User schedule analysis completed for user: {}", userId);
            return response;
        } catch (Exception e) {
            log.error("Error analyzing user schedule for user: {}", userId, e);
            throw new RuntimeException("Failed to analyze user schedule: " + e.getMessage());
        }
    }

    /**
     * Gets AI-powered suggestions for optimal scheduling.
     *
     * @param context Additional context for scheduling (e.g., team size, constraints)
     * @return Scheduling suggestions from the AI agent
     */
    public String getSuggestedSchedule(String context) {
        try {
            String prompt = String.format(
                """
                As an AI scheduling expert, generate optimal scheduling suggestions considering:
                Context: %s
                
                Provide:
                1. Recommended shift patterns
                2. Optimal team distribution
                3. Best practices for the given context
                4. Implementation roadmap
                
                Format your response with clear sections and actionable items.
                """, context
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("AI scheduling suggestions generated");
            return response;
        } catch (Exception e) {
            log.error("Error generating scheduling suggestions", e);
            throw new RuntimeException("Failed to generate suggestions: " + e.getMessage());
        }
    }
}

