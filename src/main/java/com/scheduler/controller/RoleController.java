package com.scheduler.controller;

import com.scheduler.model.Role;
import com.scheduler.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    // Get all roles - accessible by everyone
    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    // Get only active roles
    @GetMapping("/active")
    public List<Role> getActiveRoles() {
        return roleService.getActiveRoles();
    }

    // Get role by ID
    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create a new role - only ADMIN and MANAGER
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Role> createRole(@RequestBody RoleRequest roleRequest) {
        Role role = roleService.createRole(roleRequest.getName(), roleRequest.getDescription());
        return ResponseEntity.status(201).body(role);
    }

    // Update role - only ADMIN and MANAGER
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody RoleRequest roleRequest) {
        Role role = roleService.updateRole(id, roleRequest.getName(), roleRequest.getDescription(), roleRequest.isActive());
        return ResponseEntity.ok(role);
    }

    // Delete role - only ADMIN and MANAGER
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    // Toggle role active status - only ADMIN and MANAGER
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Role> toggleRole(@PathVariable Long id) {
        Role role = roleService.toggleRoleActive(id);
        return ResponseEntity.ok(role);
    }

    // DTO for role requests
    public static class RoleRequest {
        private String name;
        private String description;
        private boolean active = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}

