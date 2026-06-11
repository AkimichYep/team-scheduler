package com.scheduler.repository;

import com.scheduler.model.ScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleTemplateRepository extends JpaRepository<ScheduleTemplate, Long> {
    List<ScheduleTemplate> findAllByActive(boolean active);
    Optional<ScheduleTemplate> findByName(String name);
    Optional<ScheduleTemplate> findByIsDefaultTrue();
}

