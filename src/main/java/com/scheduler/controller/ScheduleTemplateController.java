package com.scheduler.controller;

import com.scheduler.model.ScheduleTemplate;
import com.scheduler.service.ScheduleTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class ScheduleTemplateController {

    @Autowired
    private ScheduleTemplateService templateService;

    @GetMapping
    public ResponseEntity<List<ScheduleTemplate>> getAllActiveTemplates() {
        return ResponseEntity.ok(templateService.getActiveTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleTemplate> getTemplate(@PathVariable Long id) {
        return templateService.getTemplate(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/default/template")
    public ResponseEntity<ScheduleTemplate> getDefaultTemplate() {
        return templateService.getDefaultTemplate()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ScheduleTemplate> createTemplate(@RequestBody TemplateRequest request) {
        ScheduleTemplate template = templateService.createTemplate(
                request.getName(),
                request.getHourlyActivities(),
                request.getDescription(),
                request.isDefault()
        );
        return ResponseEntity.ok(template);
    }

    @PostMapping("/apply-to-date/{userId}/{templateId}")
    public ResponseEntity<Void> applyTemplateToDate(
            @PathVariable Long userId,
            @PathVariable Long templateId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        templateService.applyTemplateToDate(userId, templateId, localDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/apply-to-range/{userId}/{templateId}")
    public ResponseEntity<Void> applyTemplateToRange(
            @PathVariable Long userId,
            @PathVariable Long templateId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        templateService.applyTemplateToDateRange(userId, templateId, start, end);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/set-default/{userId}/{templateId}")
    public ResponseEntity<Void> setUserDefaultTemplate(
            @PathVariable Long userId,
            @PathVariable Long templateId) {
        templateService.setUserDefaultTemplate(userId, templateId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/default")
    public ResponseEntity<ScheduleTemplate> getUserDefaultTemplate(@PathVariable Long userId) {
        return templateService.getUserDefaultTemplate(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/oncall/add/{userId}")
    public ResponseEntity<Void> addOnCall(
            @PathVariable Long userId,
            @RequestBody OnCallRequest request) {
        templateService.addOnCallToDate(userId, request.getDate(), request.getStartHour(), request.getEndHour());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/oncall/remove/{userId}")
    public ResponseEntity<Void> removeOnCall(
            @PathVariable Long userId,
            @RequestBody OnCallRequest request) {
        templateService.removeOnCallFromDate(userId, request.getDate(), request.getStartHour(), request.getEndHour());
        return ResponseEntity.ok().build();
    }

    // DTOs
    public static class TemplateRequest {
        private String name;
        private String[] hourlyActivities; // 24 elements
        private String description;
        private boolean isDefault;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String[] getHourlyActivities() {
            return hourlyActivities;
        }

        public void setHourlyActivities(String[] hourlyActivities) {
            this.hourlyActivities = hourlyActivities;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public void setDefault(boolean aDefault) {
            isDefault = aDefault;
        }
    }

    public static class OnCallRequest {
        private LocalDate date;
        private int startHour;
        private int endHour;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public int getStartHour() {
            return startHour;
        }

        public void setStartHour(int startHour) {
            this.startHour = startHour;
        }

        public int getEndHour() {
            return endHour;
        }

        public void setEndHour(int endHour) {
            this.endHour = endHour;
        }
    }
}

