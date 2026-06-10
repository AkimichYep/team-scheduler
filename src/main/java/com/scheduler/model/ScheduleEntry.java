package com.scheduler.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date", "hour_of_day"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "hour_of_day", nullable = false)
    @Builder.Default
    private Integer hourOfDay = 0; // 0-23 representing the hour slot (00-01, 01-02, etc.)

    @Column(nullable = false)
    private String activity; // D (Development), S (Support), O (OnCall), V (Vacation), H (Holiday), Off

    @Builder.Default
    private String notes = "";
}

