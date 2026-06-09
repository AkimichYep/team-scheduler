package com.scheduler.service;

import com.scheduler.model.Role;
import com.scheduler.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    public Role createRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            throw new RuntimeException("Role with name '" + name + "' already exists");
        }
        Role role = Role.builder()
                .name(name)
                .description(description)
                .active(true)
                .build();
        return roleRepository.save(role);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<Role> getActiveRoles() {
        return roleRepository.findAll().stream()
                .filter(Role::isActive)
                .toList();
    }

    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    public Role updateRole(Long id, String name, String description, boolean active) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));

        if (!role.getName().equals(name) && roleRepository.existsByName(name)) {
            throw new RuntimeException("Role with name '" + name + "' already exists");
        }

        role.setName(name);
        role.setDescription(description);
        role.setActive(active);
        return roleRepository.save(role);
    }

    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }

    public Role toggleRoleActive(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        role.setActive(!role.isActive());
        return roleRepository.save(role);
    }
}

