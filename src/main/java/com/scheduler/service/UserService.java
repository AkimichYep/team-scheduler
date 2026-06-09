package com.scheduler.service;

import com.scheduler.model.Role;
import com.scheduler.model.User;
import com.scheduler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(String username, String password, Role role, String project) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password)) // Automatically hashes using BCrypt
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
        user.setRole(details.getRole());
        user.setActive(details.isActive());
        user.setProject(details.getProject());
        return userRepository.save(user);
    }
}