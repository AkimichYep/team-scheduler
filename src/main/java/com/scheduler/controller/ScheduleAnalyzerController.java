package com.scheduler.controller;

import com.scheduler.service.ScheduleAnalyzerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for AI-powered schedule analysis.
 * Handles requests for schedule optimization recommendations and insights.
 */
@Slf4j
@RestController
@RequestMapping("/api/schedule-analyzer")
@CrossOrigin(origins = "http://localhost:3000")
public class ScheduleAnalyzerController {

    private final ScheduleAnalyzerService scheduleAnalyzerService;

    public ScheduleAnalyzerController(ScheduleAnalyzerService scheduleAnalyzerService) {
        this.scheduleAnalyzerService = scheduleAnalyzerService;
    }

    /**
     * Analyzes the entire team schedule and provides recommendations.
     *
     * @return Analysis results with insights and recommendations
     */
    @GetMapping("/analyze")
    public ResponseEntity<String> analyzeSchedule() {
        try {
            log.info("Received request to analyze team schedule");
            String analysis = scheduleAnalyzerService.analyzeSchedule();
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error in analyzeSchedule endpoint", e);
            return ResponseEntity.status(500).body("Error analyzing schedule: " + e.getMessage());
        }
    }

    /**
     * Analyzes a specific user's schedule.
     *
     * @param userId The ID of the user to analyze
     * @return Personalized analysis for the user
     */
    @GetMapping("/analyze/user/{userId}")
    public ResponseEntity<String> analyzeUserSchedule(@PathVariable Long userId) {
        try {
            log.info("Received request to analyze schedule for user: {}", userId);
            String analysis = scheduleAnalyzerService.analyzeUserSchedule(userId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error in analyzeUserSchedule endpoint for userId: {}", userId, e);
            return ResponseEntity.status(500).body("Error analyzing user schedule: " + e.getMessage());
        }
    }

    /**
     * Gets AI-powered suggestions for optimal scheduling.
     *
     * @param context Additional context for scheduling (e.g., team size, constraints)
     * @return Scheduling suggestions from the AI agent
     */
    @PostMapping("/suggest")
    public ResponseEntity<String> getSuggestedSchedule(@RequestParam(defaultValue = "Standard team scheduling with no specific constraints") String context) {
        try {
            log.info("Received request for scheduling suggestions with context: {}", context);
            String suggestions = scheduleAnalyzerService.getSuggestedSchedule(context);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error in getSuggestedSchedule endpoint", e);
            return ResponseEntity.status(500).body("Error generating suggestions: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for the analyzer service.
     *
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Schedule Analyzer Service is running");
    }
}

