package com.scheduler.service;

import com.scheduler.model.Role;
import com.scheduler.model.User;
import com.scheduler.repository.UserRepository;
import com.scheduler.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(String username, String password, Long roleId, String project) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .project(project)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User updateUser(Long id, User details) {
        User user = userRepository.findById(id).orElseThrow();
        user.setUsername(details.getUsername());

        // Update role if provided
        if (details.getRole() != null && details.getRole().getId() != null) {
            Role role = roleRepository.findById(details.getRole().getId())
                    .orElseThrow(() -> new RuntimeException("Role not found with id: " + details.getRole().getId()));
            user.setRole(role);
        }

        user.setActive(details.isActive());
        user.setProject(details.getProject());
        if (details.getEmail() != null) {
            user.setEmail(details.getEmail());
        }
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void updateLastAccessTime(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            User u = user.get();
            u.setLastAccessTime(LocalDateTime.now());
            userRepository.save(u);
        }
    }
}