package com.scheduler.config;

import com.scheduler.model.Role;
import com.scheduler.model.ScheduleTemplate;
import com.scheduler.repository.RoleRepository;
import com.scheduler.repository.ScheduleTemplateRepository;
import com.scheduler.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserService userService, RoleRepository roleRepository, ScheduleTemplateRepository templateRepository) {
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

            // Initialize schedule templates if they don't exist
            if (templateRepository.count() == 0) {
                // Template 1: Early shift - Support morning, Development afternoon
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Early Shift (Morning Support)")
                        .hourlyCsv("Off,Off,S,S,S,S,S,D,D,D,D,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off")
                        .description("Support hours 02-07, Development 07-11")
                        .isDefault(true)
                        .active(true)
                        .build());

                // Template 2: Mid-morning shift
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Mid-Morning Shift")
                        .hourlyCsv("Off,Off,Off,S,S,S,S,S,D,D,D,D,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off")
                        .description("Support hours 03-08, Development 08-12")
                        .isDefault(false)
                        .active(true)
                        .build());

                // Template 3: Mid-day shift
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Mid-Day Shift")
                        .hourlyCsv("Off,Off,Off,Off,Off,Off,Off,S,S,S,S,S,D,D,D,D,Off,Off,Off,Off,Off,Off,Off,Off")
                        .description("Support hours 07-12, Development 12-16")
                        .isDefault(false)
                        .active(true)
                        .build());

                // Template 4: Afternoon split shift
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Afternoon Split (Support+Dev+Support)")
                        .hourlyCsv("Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,S,S,S,S,D,D,D,D,S,Off,Off,Off,Off,Off")
                        .description("Support 10-14, Development 14-18, Support 18-19")
                        .isDefault(false)
                        .active(true)
                        .build());

                // Template 5: Evening shift
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Evening Shift")
                        .hourlyCsv("Off,Off,Off,Off,Off,Off,Off,Off,Off,D,D,D,D,S,S,S,S,S,Off,Off,Off,Off,Off,Off")
                        .description("Development 09-13, Support 13-18")
                        .isDefault(false)
                        .active(true)
                        .build());

                // OnCall Template 1: Night OnCall (00-02 hours)
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Night OnCall (00-02)")
                        .hourlyCsv("OnCall,OnCall,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off")
                        .description("OnCall for night hours 00-02")
                        .isDefault(false)
                        .active(true)
                        .build());

                // OnCall Template 2: Evening OnCall (20-00 hours)
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Evening OnCall (20-00)")
                        .hourlyCsv("Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,OnCall,OnCall,OnCall,OnCall,OnCall")
                        .description("OnCall for evening hours 20-00")
                        .isDefault(false)
                        .active(true)
                        .build());

                // OnCall Template 3: Flexible OnCall (covers both night and evening)
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Flexible OnCall (Night+Evening)")
                        .hourlyCsv("OnCall,OnCall,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,OnCall,OnCall,OnCall,OnCall,OnCall")
                        .description("OnCall for night (00-02) and evening (20-00) - flexible schedule")
                        .isDefault(false)
                        .active(true)
                        .build());
            }

            // Add new shift templates by name if they don't already exist
            if (templateRepository.findByName("Shift A - Early (02-11)").isEmpty()) {
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Shift A - Early (02-11)")
                        .hourlyCsv("Off,Off,S,S,S,S,S,D,D,D,D,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off")
                        .description("Support 02-07, Development 07-11 (9-hour shift)")
                        .isDefault(false)
                        .active(true)
                        .build());
            }
            if (templateRepository.findByName("Shift B - Morning (03-12)").isEmpty()) {
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Shift B - Morning (03-12)")
                        .hourlyCsv("Off,Off,Off,S,S,S,S,S,D,D,D,D,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,Off")
                        .description("Support 03-08, Development 08-12 (9-hour shift)")
                        .isDefault(false)
                        .active(true)
                        .build());
            }
            if (templateRepository.findByName("Shift C - Day (07-16)").isEmpty()) {
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Shift C - Day (07-16)")
                        .hourlyCsv("Off,Off,Off,Off,Off,Off,Off,S,S,S,S,S,D,D,D,D,Off,Off,Off,Off,Off,Off,Off,Off")
                        .description("Support 07-12, Development 12-16 (9-hour shift)")
                        .isDefault(false)
                        .active(true)
                        .build());
            }
            if (templateRepository.findByName("Shift D - Afternoon (10-19)").isEmpty()) {
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Shift D - Afternoon (10-19)")
                        .hourlyCsv("Off,Off,Off,Off,Off,Off,Off,Off,Off,Off,S,S,S,S,D,D,D,D,S,Off,Off,Off,Off,Off")
                        .description("Support 10-14, Development 14-18, Support 18-19 (9-hour shift)")
                        .isDefault(false)
                        .active(true)
                        .build());
            }
            if (templateRepository.findByName("Shift E - Business (09-18)").isEmpty()) {
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Shift E - Business (09-18)")
                        .hourlyCsv("Off,Off,Off,Off,Off,Off,Off,Off,Off,D,D,D,D,S,S,S,S,S,Off,Off,Off,Off,Off,Off")
                        .description("Development 09-13, Support 13-18 (9-hour shift)")
                        .isDefault(false)
                        .active(true)
                        .build());
            }
            if (templateRepository.findByName("Full Day Leave").isEmpty()) {
                templateRepository.save(ScheduleTemplate.builder()
                        .name("Full Day Leave")
                        .hourlyCsv("Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave,Leave")
                        .description("Full day leave - all 24 hours marked as Leave")
                        .isDefault(false)
                        .active(true)
                        .build());
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

