package com.scheduler.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    private String project;

    @Builder.Default
    private boolean active = true;

    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_schedule_template_id")
    private ScheduleTemplate defaultScheduleTemplate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastAccessTime;
}