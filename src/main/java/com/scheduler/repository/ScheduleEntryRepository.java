package com.scheduler.repository;

import com.scheduler.model.ScheduleEntry;
import com.scheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {
    List<ScheduleEntry> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    Optional<ScheduleEntry> findByUserIdAndDate(Long userId, LocalDate date);
    void deleteByUserIdAndDate(Long userId, LocalDate date);

    // Hourly schedule queries
    List<ScheduleEntry> findByUserIdAndDateOrderByHourOfDay(Long userId, LocalDate date);
    List<ScheduleEntry> findByUserIdAndDateBetweenOrderByDateAscHourOfDayAsc(Long userId, LocalDate startDate, LocalDate endDate);
    Optional<ScheduleEntry> findByUserIdAndDateAndHourOfDay(Long userId, LocalDate date, Integer hourOfDay);
    void deleteByUserIdAndDateAndHourOfDay(Long userId, LocalDate date, Integer hourOfDay);
}

