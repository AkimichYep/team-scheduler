package com.scheduler.service;

import com.scheduler.model.ScheduleEntry;
import com.scheduler.model.User;
import com.scheduler.repository.ScheduleEntryRepository;
import com.scheduler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ScheduleEntry> getScheduleForMonth(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<ScheduleEntry> entries = scheduleEntryRepository.findByUserIdAndDateBetweenOrderByDateAscHourOfDayAsc(userId, startDate, endDate);
        User user = userRepository.findById(userId).orElseThrow();

        // Initialize default entries for all hours of days that don't have entries
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            for (int hour = 0; hour < 24; hour++) {
                final int h = hour;
                if (entries.stream().noneMatch(e -> e.getDate().equals(currentDate) && e.getHourOfDay() == h)) {
                    String activity = isWeekend(currentDate) ? "Off" : "D";
                    ScheduleEntry entry = ScheduleEntry.builder()
                            .user(user)
                            .date(currentDate)
                            .hourOfDay(hour)
                            .activity(activity)
                            .notes("")
                            .build();
                    entries.add(entry);
                }
            }
        }
        
        entries.sort((a, b) -> {
            int dateCompare = a.getDate().compareTo(b.getDate());
            if (dateCompare != 0) return dateCompare;
            return a.getHourOfDay().compareTo(b.getHourOfDay());
        });
        return entries;
    }

    public List<ScheduleEntry> getScheduleForWeek(Long userId, LocalDate date) {
        LocalDate startOfWeek = date.minusDays(date.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        List<ScheduleEntry> entries = scheduleEntryRepository.findByUserIdAndDateBetween(userId, startOfWeek, endOfWeek);
        
        // Initialize default entries for days that don't have entries
        for (LocalDate d = startOfWeek; !d.isAfter(endOfWeek); d = d.plusDays(1)) {
            final LocalDate currentDate = d;
            if (entries.stream().noneMatch(e -> e.getDate().equals(currentDate))) {
                User user = userRepository.findById(userId).orElseThrow();
                String activity = isWeekend(currentDate) ? "Off" : "D";
                ScheduleEntry entry = ScheduleEntry.builder()
                        .user(user)
                        .date(currentDate)
                        .activity(activity)
                        .notes("")
                        .build();
                entries.add(entry);
            }
        }
        
        entries.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return entries;
    }

    public Optional<ScheduleEntry> getScheduleForDay(Long userId, LocalDate date) {
        Optional<ScheduleEntry> entry = scheduleEntryRepository.findByUserIdAndDate(userId, date);
        if (entry.isEmpty()) {
            User user = userRepository.findById(userId).orElseThrow();
            String activity = isWeekend(date) ? "Off" : "D";
            return Optional.of(ScheduleEntry.builder()
                    .user(user)
                    .date(date)
                    .activity(activity)
                    .notes("")
                    .build());
        }
        return entry;
    }

    // Get all 24 hours for a specific day
    public List<ScheduleEntry> getScheduleForDayAllHours(Long userId, LocalDate date) {
        List<ScheduleEntry> entries = scheduleEntryRepository.findByUserIdAndDateOrderByHourOfDay(userId, date);
        User user = userRepository.findById(userId).orElseThrow();

        // Initialize default entries for hours that don't have entries
        for (int hour = 0; hour < 24; hour++) {
            final int h = hour;
            if (entries.stream().noneMatch(e -> e.getHourOfDay() == h)) {
                String activity = isWeekend(date) ? "Off" : "D";
                ScheduleEntry entry = ScheduleEntry.builder()
                        .user(user)
                        .date(date)
                        .hourOfDay(hour)
                        .activity(activity)
                        .notes("")
                        .build();
                entries.add(entry);
            }
        }

        entries.sort((a, b) -> a.getHourOfDay().compareTo(b.getHourOfDay()));
        return entries;
    }

    // Get daily summary for a specific date range (week before, current month, week after)
    public Map<String, List<ScheduleEntry>> getDailySummary(Long userId, LocalDate referenceDate) {
        Map<String, List<ScheduleEntry>> summary = new HashMap<>();

        // Week before
        LocalDate weekBeforeStart = referenceDate.minusDays(13);
        LocalDate weekBeforeEnd = referenceDate.minusDays(7);
        List<ScheduleEntry> weekBefore = scheduleEntryRepository.findByUserIdAndDateBetweenOrderByDateAscHourOfDayAsc(userId, weekBeforeStart, weekBeforeEnd);
        initializeHourlyEntries(userId, weekBeforeStart, weekBeforeEnd, weekBefore);
        summary.put("weekBefore", weekBefore);

        // Current month
        YearMonth yearMonth = YearMonth.from(referenceDate);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        List<ScheduleEntry> currentMonth = scheduleEntryRepository.findByUserIdAndDateBetweenOrderByDateAscHourOfDayAsc(userId, monthStart, monthEnd);
        initializeHourlyEntries(userId, monthStart, monthEnd, currentMonth);
        summary.put("currentMonth", currentMonth);

        // Week after
        LocalDate weekAfterStart = referenceDate.plusDays(7);
        LocalDate weekAfterEnd = referenceDate.plusDays(13);
        List<ScheduleEntry> weekAfter = scheduleEntryRepository.findByUserIdAndDateBetweenOrderByDateAscHourOfDayAsc(userId, weekAfterStart, weekAfterEnd);
        initializeHourlyEntries(userId, weekAfterStart, weekAfterEnd, weekAfter);
        summary.put("weekAfter", weekAfter);

        return summary;
    }

    private void initializeHourlyEntries(Long userId, LocalDate startDate, LocalDate endDate, List<ScheduleEntry> entries) {
        User user = userRepository.findById(userId).orElseThrow();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            for (int hour = 0; hour < 24; hour++) {
                final int h = hour;
                if (entries.stream().noneMatch(e -> e.getDate().equals(currentDate) && e.getHourOfDay() == h)) {
                    String activity = isWeekend(currentDate) ? "Off" : "D";
                    ScheduleEntry entry = ScheduleEntry.builder()
                            .user(user)
                            .date(currentDate)
                            .hourOfDay(hour)
                            .activity(activity)
                            .notes("")
                            .build();
                    entries.add(entry);
                }
            }
        }
    }

    public ScheduleEntry updateScheduleEntry(Long userId, LocalDate date, String activity, String notes) {
        User user = userRepository.findById(userId).orElseThrow();

        // When no hour is specified, update ALL 24 hours for that day
        ScheduleEntry lastUpdated = null;
        for (int hour = 0; hour < 24; hour++) {
            lastUpdated = updateScheduleEntry(userId, date, hour, activity, notes);
        }

        return lastUpdated;
    }

    public ScheduleEntry updateScheduleEntry(Long userId, LocalDate date, Integer hour, String activity, String notes) {
        User user = userRepository.findById(userId).orElseThrow();

        Optional<ScheduleEntry> existingEntry = scheduleEntryRepository.findByUserIdAndDateAndHourOfDay(userId, date, hour);

        ScheduleEntry entry;
        if (existingEntry.isPresent()) {
            entry = existingEntry.get();
            entry.setActivity(activity);
            entry.setNotes(notes != null ? notes : "");
        } else {
            entry = ScheduleEntry.builder()
                    .user(user)
                    .date(date)
                    .hourOfDay(hour)
                    .activity(activity)
                    .notes(notes != null ? notes : "")
                    .build();
        }
        
        return scheduleEntryRepository.save(entry);
    }

    public void deleteScheduleEntry(Long userId, LocalDate date) {
        scheduleEntryRepository.deleteByUserIdAndDate(userId, date);
    }

    public void deleteScheduleEntry(Long userId, LocalDate date, Integer hour) {
        scheduleEntryRepository.deleteByUserIdAndDateAndHourOfDay(userId, date, hour);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public List<ScheduleEntry> getScheduleForYear(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        return scheduleEntryRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }
}

