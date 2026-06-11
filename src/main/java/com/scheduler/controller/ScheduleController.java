package com.scheduler.controller;

import com.scheduler.model.ScheduleEntry;
import com.scheduler.model.User;
import com.scheduler.service.ScheduleService;
import com.scheduler.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private UserService userService;

    @GetMapping("/month/{userId}/{year}/{month}")
    public ResponseEntity<List<ScheduleEntry>> getMonthSchedule(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        List<ScheduleEntry> schedule = scheduleService.getScheduleForMonth(userId, year, month);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/week/{userId}")
    public ResponseEntity<List<ScheduleEntry>> getWeekSchedule(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<ScheduleEntry> schedule = scheduleService.getScheduleForWeek(userId, localDate);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/day/{userId}")
    public ResponseEntity<ScheduleEntry> getDaySchedule(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        ScheduleEntry schedule = scheduleService.getScheduleForDay(userId, localDate)
                .orElseThrow();
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/day/{userId}/hours")
    public ResponseEntity<List<ScheduleEntry>> getDayScheduleAllHours(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<ScheduleEntry> schedule = scheduleService.getScheduleForDayAllHours(userId, localDate);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/year/{userId}/{year}")
    public ResponseEntity<List<ScheduleEntry>> getYearSchedule(
            @PathVariable Long userId,
            @PathVariable int year) {
        List<ScheduleEntry> schedule = scheduleService.getScheduleForYear(userId, year);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ScheduleEntry> updateScheduleEntry(
            @PathVariable Long userId,
            @RequestBody ScheduleEntryRequest request) {

        ScheduleEntry entry;
        if (request.getHour() != null) {
            // Hourly view: update specific hour only
            entry = scheduleService.updateScheduleEntry(
                    userId,
                    LocalDate.parse(request.getDate()),
                    request.getHour(),
                    request.getActivity(),
                    request.getIsOnCall(),
                    request.getOnCallMorning(),
                    request.getOnCallNight(),
                    request.getNotes()
            );
        } else {
            // Day view: update all 24 hours for the day
            entry = scheduleService.updateScheduleEntry(
                    userId,
                    LocalDate.parse(request.getDate()),
                    request.getActivity(),
                    request.getIsOnCall(),
                    request.getOnCallMorning(),
                    request.getOnCallNight(),
                    request.getNotes()
            );
        }
        return ResponseEntity.ok(entry);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteScheduleEntry(
            @PathVariable Long userId,
            @RequestParam String date,
            @RequestParam(required = false) Integer hour) {
        LocalDate localDate = LocalDate.parse(date);
        if (hour != null) {
            scheduleService.deleteScheduleEntry(userId, localDate, hour);
        } else {
            scheduleService.deleteScheduleEntry(userId, localDate);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/daily-summary/{userId}")
    public ResponseEntity<Map<String, List<ScheduleEntry>>> getDailySummary(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate referenceDate = LocalDate.parse(date);
        Map<String, List<ScheduleEntry>> summary = scheduleService.getDailySummary(userId, referenceDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/team/month/{year}/{month}")
    public ResponseEntity<Map<Long, List<ScheduleEntry>>> getTeamMonthSchedule(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(required = false) List<Long> userIds) {
        Map<Long, List<ScheduleEntry>> teamSchedule = new HashMap<>();

        List<User> users;
        if (userIds != null && !userIds.isEmpty()) {
            users = userService.getUsersByIds(userIds);
        } else {
            users = userService.getAllUsers();
        }

        for (User user : users) {
            List<ScheduleEntry> schedule = scheduleService.getScheduleForMonth(user.getId(), year, month);
            teamSchedule.put(user.getId(), schedule);
        }

        return ResponseEntity.ok(teamSchedule);
    }

    @GetMapping("/all-users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // DTO for schedule entry requests
    public static class ScheduleEntryRequest {
        private String date;
        private Integer hour;
        private String activity;
        private Boolean isOnCall;
        private Boolean onCallMorning;
        private Boolean onCallNight;
        private String notes;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Integer getHour() {
            return hour;
        }

        public void setHour(Integer hour) {
            this.hour = hour;
        }

        public String getActivity() {
            return activity;
        }

        public void setActivity(String activity) {
            this.activity = activity;
        }

        public Boolean getIsOnCall() {
            return isOnCall != null ? isOnCall : false;
        }

        public void setIsOnCall(Boolean isOnCall) {
            this.isOnCall = isOnCall;
        }

        public Boolean getOnCallMorning() {
            return onCallMorning != null ? onCallMorning : false;
        }

        public void setOnCallMorning(Boolean onCallMorning) {
            this.onCallMorning = onCallMorning;
        }

        public Boolean getOnCallNight() {
            return onCallNight != null ? onCallNight : false;
        }

        public void setOnCallNight(Boolean onCallNight) {
            this.onCallNight = onCallNight;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}

