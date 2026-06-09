package com.scheduler.config;

import com.scheduler.model.Role;
import com.scheduler.repository.RoleRepository;
import com.scheduler.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserService userService, RoleRepository roleRepository) {
        return args -> {
            // Initialize roles if they don't exist
            if (roleRepository.findByName("ADMIN").isEmpty()) {
                roleRepository.save(Role.builder().name("ADMIN").description("Administrator role with full access").active(true).build());
            }
            if (roleRepository.findByName("MANAGER").isEmpty()) {
                roleRepository.save(Role.builder().name("MANAGER").description("Manager role with limited administrative access").active(true).build());
            }
            if (roleRepository.findByName("L1_SUPPORT").isEmpty()) {
                roleRepository.save(Role.builder().name("L1_SUPPORT").description("Level 1 Support").active(true).build());
            }
            if (roleRepository.findByName("L2_SUPPORT").isEmpty()) {
                roleRepository.save(Role.builder().name("L2_SUPPORT").description("Level 2 Support").active(true).build());
            }
            if (roleRepository.findByName("L3_DEVELOPERS").isEmpty()) {
                roleRepository.save(Role.builder().name("L3_DEVELOPERS").description("Level 3 Developers").active(true).build());
            }

            // Create default users if they don't exist
            if (userService.getAllUsers().isEmpty()) {
                Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
                Role managerRole = roleRepository.findByName("MANAGER").orElseThrow();
                Role devRole = roleRepository.findByName("L3_DEVELOPERS").orElseThrow();

                userService.createUser("admin", "pass", adminRole.getId(), "Global");
                userService.createUser("manager_a", "pass", managerRole.getId(), "Project-Alpha");
                userService.createUser("dev_one", "pass", devRole.getId(), "Project-Alpha");
            }
        };
    }
}

