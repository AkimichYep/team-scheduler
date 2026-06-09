package com.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class TeamSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamSchedulerApplication.class, args);
    }
}