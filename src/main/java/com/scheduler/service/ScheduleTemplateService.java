package com.scheduler.service;

import com.scheduler.model.ScheduleEntry;
import com.scheduler.model.ScheduleTemplate;
import com.scheduler.model.User;
import com.scheduler.repository.ScheduleEntryRepository;
import com.scheduler.repository.ScheduleTemplateRepository;
import com.scheduler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduleTemplateService {

    @Autowired
    private ScheduleTemplateRepository templateRepository;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all active templates
     */
    public List<ScheduleTemplate> getActiveTemplates() {
        return templateRepository.findAllByActive(true);
    }

    /**
     * Get template by ID
     */
    public Optional<ScheduleTemplate> getTemplate(Long templateId) {
        return templateRepository.findById(templateId);
    }

    /**
     * Get default template
     */
    public Optional<ScheduleTemplate> getDefaultTemplate() {
        return templateRepository.findByIsDefaultTrue();
    }

    /**
     * Create a new template
     */
    public ScheduleTemplate createTemplate(String name, String[] hourlyActivities, String description, boolean isDefault) {
        if (hourlyActivities.length != 24) {
            throw new IllegalArgumentException("Must provide exactly 24 hourly activities");
        }

        // If this is set as default, unset any existing default
        if (isDefault) {
            templateRepository.findByIsDefaultTrue().ifPresent(existing -> {
                existing.setDefault(false);
                templateRepository.save(existing);
            });
        }

        ScheduleTemplate template = ScheduleTemplate.builder()
                .name(name)
                .hourlyCsv(String.join(",", hourlyActivities))
                .description(description)
                .isDefault(isDefault)
                .active(true)
                .build();

        return templateRepository.save(template);
    }

    /**
     * Apply a template to a specific date for a user
     */
    public void applyTemplateToDate(Long userId, Long templateId, LocalDate date) {
        ScheduleTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String[] activities = template.getHourlyActivities();

        for (int hour = 0; hour < 24; hour++) {
            String activity = activities[hour];
            Optional<ScheduleEntry> existing = scheduleEntryRepository.findByUserIdAndDateAndHourOfDay(userId, date, hour);

            ScheduleEntry entry;
            if (existing.isPresent()) {
                entry = existing.get();
                entry.setActivity(activity);
            } else {
                entry = ScheduleEntry.builder()
                        .user(user)
                        .date(date)
                        .hourOfDay(hour)
                        .activity(activity)
                        .notes("")
                        .build();
            }
            scheduleEntryRepository.save(entry);
        }
    }

    /**
     * Apply a template to a date range for a user
     */
    public void applyTemplateToDateRange(Long userId, Long templateId, LocalDate startDate, LocalDate endDate) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            applyTemplateToDate(userId, templateId, date);
        }
    }

    /**
     * Apply a template to specific days of a week for the next N weeks
     */
    public void applyTemplateToWeeklyDays(Long userId, Long templateId, LocalDate startDate, int numberOfWeeks, int... daysOfWeek) {
        // daysOfWeek: 0=Sunday, 1=Monday, ..., 6=Saturday
        for (int week = 0; week < numberOfWeeks; week++) {
            for (int dayOfWeek : daysOfWeek) {
                LocalDate targetDate = startDate.plusWeeks(week);
                // Adjust to the target day of week
                int currentDayOfWeek = targetDate.getDayOfWeek().getValue() % 7; // 0=Sunday
                int daysToAdd = (dayOfWeek - currentDayOfWeek + 7) % 7;
                targetDate = targetDate.plusDays(daysToAdd);
                applyTemplateToDate(userId, templateId, targetDate);
            }
        }
    }

    /**
     * Set user's default template
     */
    public void setUserDefaultTemplate(Long userId, Long templateId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ScheduleTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        user.setDefaultScheduleTemplate(template);
        userRepository.save(user);
    }

    /**
     * Get user's default template
     */
    public Optional<ScheduleTemplate> getUserDefaultTemplate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return Optional.ofNullable(user.getDefaultScheduleTemplate());
    }

    /**
     * Merge OnCall activity with existing schedule
     * OnCall doesn't override other activities, it adds to them
     */
    public void addOnCallToDate(Long userId, LocalDate date, int startHour, int endHour) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        for (int hour = startHour; hour < endHour && hour < 24; hour++) {
            Optional<ScheduleEntry> existing = scheduleEntryRepository.findByUserIdAndDateAndHourOfDay(userId, date, hour);

            ScheduleEntry entry;
            String currentActivity = "Off";
            if (existing.isPresent()) {
                entry = existing.get();
                currentActivity = entry.getActivity();
            } else {
                entry = ScheduleEntry.builder()
                        .user(user)
                        .date(date)
                        .hourOfDay(hour)
                        .notes("")
                        .build();
            }

            // Add OnCall note but keep base activity - unless it's Off, then make it OnCall
            if ("Off".equals(currentActivity)) {
                entry.setActivity("OnCall");
            } else {
                // Keep the base activity but add OnCall to notes
                String notes = entry.getNotes() != null ? entry.getNotes() : "";
                if (!notes.contains("OnCall")) {
                    entry.setNotes(notes.isEmpty() ? "OnCall" : notes + ", OnCall");
                }
            }

            scheduleEntryRepository.save(entry);
        }
    }

    /**
     * Remove OnCall from specific date/hours
     */
    public void removeOnCallFromDate(Long userId, LocalDate date, int startHour, int endHour) {
        for (int hour = startHour; hour < endHour && hour < 24; hour++) {
            Optional<ScheduleEntry> existing = scheduleEntryRepository.findByUserIdAndDateAndHourOfDay(userId, date, hour);

            if (existing.isPresent()) {
                ScheduleEntry entry = existing.get();
                if ("OnCall".equals(entry.getActivity())) {
                    entry.setActivity("Off");
                }
                String notes = entry.getNotes() != null ? entry.getNotes() : "";
                if (notes.contains("OnCall")) {
                    entry.setNotes(notes.replace(", OnCall", "").replace("OnCall", "").trim());
                }
                scheduleEntryRepository.save(entry);
            }
        }
    }
}

