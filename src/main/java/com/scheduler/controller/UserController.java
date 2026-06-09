package com.scheduler.controller;

import com.scheduler.model.User;
import com.scheduler.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Get all users
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Create a new user (Admin/Manager only)
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getProject()
        );
        return ResponseEntity.ok(createdUser);
    }
}