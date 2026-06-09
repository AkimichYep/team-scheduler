package com.scheduler.config;

import com.scheduler.model.Role;
import com.scheduler.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserService userService) {
        return args -> {
            userService.createUser("admin", "pass", Role.ADMIN, "Global");
            userService.createUser("manager_a", "pass", Role.MANAGER, "Project-Alpha");
            userService.createUser("dev_one", "pass", Role.L3_DEVELOPERS, "Project-Alpha");
        };
    }
}