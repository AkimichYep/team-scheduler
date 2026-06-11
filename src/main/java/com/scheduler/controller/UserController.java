package com.scheduler.controller;

import com.scheduler.model.User;
import com.scheduler.repository.UserRepository;
import com.scheduler.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // Get all users (without passwords)
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // Create a new user (Admin/Manager only)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest userRequest) {
        User createdUser = userService.createUser(
                userRequest.getUsername(),
                userRequest.getPassword(),
                userRequest.getRoleId(),
                userRequest.getProject()
        );
        return ResponseEntity.status(201).body(toResponse(createdUser));
    }

    // Only Admin can delete
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Admin and Manager can edit
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        return ResponseEntity.ok(toResponse(userService.updateUser(id, userDetails)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(userRepository.findById(id).orElseThrow()));
    }

    @PutMapping("/{username}/update-access-time")
    public ResponseEntity<Void> updateAccessTime(@PathVariable String username) {
        userService.updateLastAccessTime(username);
        return ResponseEntity.ok().build();
    }

    // Helper method to convert User to UserResponse (without password)
    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setProject(user.getProject());
        response.setActive(user.isActive());
        response.setEmail(user.getEmail());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastAccessTime(user.getLastAccessTime());
        response.setDefaultScheduleTemplate(user.getDefaultScheduleTemplate());
        return response;
    }

    // DTO for user requests
    public static class UserRequest {
        private String username;
        private String password;
        private Long roleId;
        private String project;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }
    }

    // Lightweight response DTO without password
    public static class UserResponse {
        private Long id;
        private String username;
        private com.scheduler.model.Role role;
        private String project;
        private boolean active;
        private String email;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime lastAccessTime;
        private com.scheduler.model.ScheduleTemplate defaultScheduleTemplate;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public com.scheduler.model.Role getRole() {
            return role;
        }

        public void setRole(com.scheduler.model.Role role) {
            this.role = role;
        }

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public java.time.LocalDateTime getLastAccessTime() {
            return lastAccessTime;
        }

        public void setLastAccessTime(java.time.LocalDateTime lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }

        public com.scheduler.model.ScheduleTemplate getDefaultScheduleTemplate() {
            return defaultScheduleTemplate;
        }

        public void setDefaultScheduleTemplate(com.scheduler.model.ScheduleTemplate defaultScheduleTemplate) {
            this.defaultScheduleTemplate = defaultScheduleTemplate;
        }
    }
}

