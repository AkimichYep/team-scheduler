package com.scheduler.service;

import com.scheduler.model.Role;
import com.scheduler.model.User;
import com.scheduler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User createUser(String username, String password, Role role, String project) {
        User user = User.builder()
                .username(username)
                .password(password) // Note: In production, encode this with BCrypt!
                .role(role)
                .project(project)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}