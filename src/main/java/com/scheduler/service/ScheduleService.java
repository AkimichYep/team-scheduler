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
import java.util.List;
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
        
        List<ScheduleEntry> entries = scheduleEntryRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        
        // Initialize default entries for days that don't have entries
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
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

    public ScheduleEntry updateScheduleEntry(Long userId, LocalDate date, String activity, String notes) {
        User user = userRepository.findById(userId).orElseThrow();
        
        Optional<ScheduleEntry> existingEntry = scheduleEntryRepository.findByUserIdAndDate(userId, date);
        
        ScheduleEntry entry;
        if (existingEntry.isPresent()) {
            entry = existingEntry.get();
            entry.setActivity(activity);
            entry.setNotes(notes != null ? notes : "");
        } else {
            entry = ScheduleEntry.builder()
                    .user(user)
                    .date(date)
                    .activity(activity)
                    .notes(notes != null ? notes : "")
                    .build();
        }
        
        return scheduleEntryRepository.save(entry);
    }

    public void deleteScheduleEntry(Long userId, LocalDate date) {
        scheduleEntryRepository.deleteByUserIdAndDate(userId, date);
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

