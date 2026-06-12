package com.scheduler.controller;

import com.scheduler.model.ScheduleEntry;
import com.scheduler.model.User;
import com.scheduler.service.ScheduleService;
import com.scheduler.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "scheduleMonth", key = "#userId + ':' + #year + ':' + #month", unless = "#result == null or #result.body == null")
    public ResponseEntity<List<ScheduleEntryResponse>> getMonthSchedule(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        List<ScheduleEntry> schedule = scheduleService.getScheduleForMonth(userId, year, month);
        return ResponseEntity.ok(toResponseList(schedule));
    }

    @GetMapping("/week/{userId}")
    public ResponseEntity<List<ScheduleEntryResponse>> getWeekSchedule(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<ScheduleEntry> schedule = scheduleService.getScheduleForWeek(userId, localDate);
        return ResponseEntity.ok(toResponseList(schedule));
    }

    @GetMapping("/day/{userId}")
    public ResponseEntity<ScheduleEntryResponse> getDaySchedule(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        ScheduleEntry schedule = scheduleService.getScheduleForDay(userId, localDate)
                .orElseThrow();
        return ResponseEntity.ok(toResponse(schedule));
    }

    @GetMapping("/day/{userId}/hours")
    @Cacheable(value = "scheduleDayHours", key = "#userId + ':' + #date", unless = "#result == null or #result.body == null")
    public ResponseEntity<List<ScheduleEntryResponse>> getDayScheduleAllHours(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<ScheduleEntry> schedule = scheduleService.getScheduleForDayAllHours(userId, localDate);
        return ResponseEntity.ok(toResponseList(schedule));
    }

    @GetMapping("/year/{userId}/{year}")
    public ResponseEntity<List<ScheduleEntryResponse>> getYearSchedule(
            @PathVariable Long userId,
            @PathVariable int year) {
        List<ScheduleEntry> schedule = scheduleService.getScheduleForYear(userId, year);
        return ResponseEntity.ok(toResponseList(schedule));
    }

    @PostMapping("/{userId}")
    @CacheEvict(value = {"scheduleMonth", "scheduleTeamMonth", "scheduleDayHours"}, allEntries = true)
    public ResponseEntity<?> updateScheduleEntry(
            @PathVariable Long userId,
            @RequestBody ScheduleEntryRequest request) {

        // Handle batched hours (optimized request with multiple hours)
        if (request.getHours() != null && !request.getHours().isEmpty()) {
            LocalDate date = LocalDate.parse(request.getDate());
            for (HourEntry hourEntry : request.getHours()) {
                scheduleService.updateScheduleEntry(
                        userId,
                        date,
                        hourEntry.getHour(),
                        hourEntry.getActivity(),
                        false,
                        false,
                        false,
                        hourEntry.getHour() == 0 ? (request.getNotes() != null ? request.getNotes() : "") : ""
                );
            }
            // Return the last updated entry or a summary
            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("status", "success");
                put("date", request.getDate());
                put("hoursUpdated", String.valueOf(request.getHours().size()));
            }});
        }

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
    @CacheEvict(value = {"scheduleMonth", "scheduleTeamMonth", "scheduleDayHours"}, allEntries = true)
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
    public ResponseEntity<Map<String, List<ScheduleEntryResponse>>> getDailySummary(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate referenceDate = LocalDate.parse(date);
        Map<String, List<ScheduleEntry>> summary = scheduleService.getDailySummary(userId, referenceDate);
        Map<String, List<ScheduleEntryResponse>> responseSummary = new HashMap<>();
        for (Map.Entry<String, List<ScheduleEntry>> entry : summary.entrySet()) {
            responseSummary.put(entry.getKey(), toResponseList(entry.getValue()));
        }
        return ResponseEntity.ok(responseSummary);
    }

    @GetMapping("/all-users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/team/week/{startDate}")
    public ResponseEntity<Map<Long, List<GroupedScheduleEntryResponse>>> getTeamWeekSchedule(
            @PathVariable String startDate,
            @RequestParam(required = false) List<Long> userIds) {
        LocalDate weekStart = LocalDate.parse(startDate);
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<Long, List<GroupedScheduleEntryResponse>> teamSchedule = new HashMap<>();

        List<User> users;
        if (userIds != null && !userIds.isEmpty()) {
            users = userService.getUsersByIds(userIds);
        } else {
            users = userService.getAllUsers();
        }

        for (User user : users) {
            List<ScheduleEntry> schedule = scheduleService.getScheduleForDateRange(user.getId(), weekStart, weekEnd);
            teamSchedule.put(user.getId(), toGroupedResponseList(schedule));
        }

        return ResponseEntity.ok(teamSchedule);
    }

    @GetMapping("/team/month/{year}/{month}")
    public ResponseEntity<Map<Long, List<GroupedScheduleEntryResponse>>> getTeamMonthSchedule(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(required = false) List<Long> userIds) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        Map<Long, List<GroupedScheduleEntryResponse>> teamSchedule = new HashMap<>();

        List<User> users;
        if (userIds != null && !userIds.isEmpty()) {
            users = userService.getUsersByIds(userIds);
        } else {
            users = userService.getAllUsers();
        }

        for (User user : users) {
            List<ScheduleEntry> schedule = scheduleService.getScheduleForDateRange(user.getId(), monthStart, monthEnd);
            teamSchedule.put(user.getId(), toGroupedResponseList(schedule));
        }

        return ResponseEntity.ok(teamSchedule);
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
        private java.util.List<HourEntry> hours;

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

        public java.util.List<HourEntry> getHours() {
            return hours;
        }

        public void setHours(java.util.List<HourEntry> hours) {
            this.hours = hours;
        }
    }

    public static class HourEntry {
        private Integer hour;
        private String activity;

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
    }

    // Helper methods to convert ScheduleEntry to lightweight response
    private ScheduleEntryResponse toResponse(ScheduleEntry entry) {
        ScheduleEntryResponse response = new ScheduleEntryResponse();
        response.setDate(entry.getDate());
        response.setHourOfDay(entry.getHourOfDay());
        response.setActivity(entry.getActivity());
        response.setIsOnCall(entry.getIsOnCall());
        response.setOnCallMorning(entry.getOnCallMorning());
        response.setOnCallNight(entry.getOnCallNight());
        response.setNotes(entry.getNotes());
        return response;
    }

    private List<ScheduleEntryResponse> toResponseList(List<ScheduleEntry> entries) {
        return entries.stream().map(this::toResponse).collect(java.util.stream.Collectors.toList());
    }

    // Group consecutive hours with same activity to reduce payload
    private List<GroupedScheduleEntryResponse> toGroupedResponseList(List<ScheduleEntry> entries) {
        if (entries == null || entries.isEmpty()) return new java.util.ArrayList<>();

        // Sort by date and hour to ensure consecutive hours are grouped
        List<ScheduleEntry> sorted = entries.stream()
            .sorted((a, b) -> {
                int dateCompare = a.getDate().compareTo(b.getDate());
                if (dateCompare != 0) return dateCompare;
                return Integer.compare(a.getHourOfDay(), b.getHourOfDay());
            })
            .collect(java.util.stream.Collectors.toList());

        List<GroupedScheduleEntryResponse> grouped = new java.util.ArrayList<>();
        int groupStartHour = 0;
        ScheduleEntry groupEntry = null;

        for (int i = 0; i < sorted.size(); i++) {
            ScheduleEntry current = sorted.get(i);

            // Start new group if first entry or activity changed or date changed
            if (groupEntry == null ||
                !current.getDate().equals(groupEntry.getDate()) ||
                !current.getActivity().equals(groupEntry.getActivity()) ||
                !java.util.Objects.equals(current.getIsOnCall(), groupEntry.getIsOnCall())) {

                // Save previous group if exists
                if (groupEntry != null) {
                    grouped.add(new GroupedScheduleEntryResponse(
                        groupEntry.getDate(),
                        groupStartHour,
                        (i > 0 ? sorted.get(i - 1).getHourOfDay() : groupStartHour),
                        groupEntry.getActivity(),
                        groupEntry.getIsOnCall(),
                        groupEntry.getOnCallMorning(),
                        groupEntry.getOnCallNight(),
                        groupEntry.getNotes()
                    ));
                } else if (current.getHourOfDay() > 0) {
                    // Add missing Off hours at beginning of first date
                    grouped.add(new GroupedScheduleEntryResponse(
                        current.getDate(), 0, current.getHourOfDay() - 1,
                        "Off", false, false, false, ""
                    ));
                }

                groupStartHour = current.getHourOfDay();
                groupEntry = current;
            }

            // If this is the last entry, save it
            if (i == sorted.size() - 1) {
                grouped.add(new GroupedScheduleEntryResponse(
                    current.getDate(),
                    groupStartHour,
                    current.getHourOfDay(),
                    current.getActivity(),
                    current.getIsOnCall(),
                    current.getOnCallMorning(),
                    current.getOnCallNight(),
                    current.getNotes()
                ));
            }
        }

        return grouped;
    }

    // Grouped response DTO for optimized API responses
    public static class GroupedScheduleEntryResponse {
        private java.time.LocalDate date;
        private String hourRange; // e.g., "0-3" for hours 0,1,2,3 or "0" for single hour
        private String activity;
        private Boolean isOnCall;
        private Boolean onCallMorning;
        private Boolean onCallNight;
        private String notes;

        public GroupedScheduleEntryResponse(java.time.LocalDate date, int startHour, int endHour, String activity, Boolean isOnCall, Boolean onCallMorning, Boolean onCallNight, String notes) {
            this.date = date;
            this.hourRange = startHour == endHour ? String.valueOf(startHour) : startHour + "-" + endHour;
            this.activity = activity;
            this.isOnCall = isOnCall;
            this.onCallMorning = onCallMorning;
            this.onCallNight = onCallNight;
            this.notes = notes;
        }

        public java.time.LocalDate getDate() { return date; }
        public void setDate(java.time.LocalDate date) { this.date = date; }
        public String getHourRange() { return hourRange; }
        public void setHourRange(String hourRange) { this.hourRange = hourRange; }
        public String getActivity() { return activity; }
        public void setActivity(String activity) { this.activity = activity; }
        public Boolean getIsOnCall() { return isOnCall; }
        public void setIsOnCall(Boolean isOnCall) { this.isOnCall = isOnCall; }
        public Boolean getOnCallMorning() { return onCallMorning; }
        public void setOnCallMorning(Boolean onCallMorning) { this.onCallMorning = onCallMorning; }
        public Boolean getOnCallNight() { return onCallNight; }
        public void setOnCallNight(Boolean onCallNight) { this.onCallNight = onCallNight; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    // Lightweight response DTO without user object
    public static class ScheduleEntryResponse {
        private java.time.LocalDate date;
        private Integer hourOfDay;
        private String activity;
        private Boolean isOnCall;
        private Boolean onCallMorning;
        private Boolean onCallNight;
        private String notes;

        public java.time.LocalDate getDate() {
            return date;
        }

        public void setDate(java.time.LocalDate date) {
            this.date = date;
        }

        public Integer getHourOfDay() {
            return hourOfDay;
        }

        public void setHourOfDay(Integer hourOfDay) {
            this.hourOfDay = hourOfDay;
        }

        public String getActivity() {
            return activity;
        }

        public void setActivity(String activity) {
            this.activity = activity;
        }

        public Boolean getIsOnCall() {
            return isOnCall;
        }

        public void setIsOnCall(Boolean isOnCall) {
            this.isOnCall = isOnCall;
        }

        public Boolean getOnCallMorning() {
            return onCallMorning;
        }

        public void setOnCallMorning(Boolean onCallMorning) {
            this.onCallMorning = onCallMorning;
        }

        public Boolean getOnCallNight() {
            return onCallNight;
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

