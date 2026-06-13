package com.scheduler.dto;

import com.scheduler.model.Role;
import com.scheduler.model.ScheduleTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private Role role;
    private String project;
    private boolean active;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessTime;
    private ScheduleTemplate defaultScheduleTemplate;
}
