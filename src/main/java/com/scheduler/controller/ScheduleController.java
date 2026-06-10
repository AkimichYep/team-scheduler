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
        ScheduleEntry entry = scheduleService.updateScheduleEntry(
                userId,
                LocalDate.parse(request.getDate()),
                request.getActivity(),
                request.getNotes()
        );
        return ResponseEntity.ok(entry);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteScheduleEntry(
            @PathVariable Long userId,
            @RequestParam String date) {
        scheduleService.deleteScheduleEntry(userId, LocalDate.parse(date));
        return ResponseEntity.noContent().build();
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
        private String activity;
        private String notes;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getActivity() {
            return activity;
        }

        public void setActivity(String activity) {
            this.activity = activity;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}

