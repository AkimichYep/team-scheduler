package com.scheduler.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schedule_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 500)
    private String hourlyCsv; // Comma-separated values for 24 hours (e.g., "Off,Off,Spt,Spt,Spt,Spt,Spt,Dev,Dev,Dev,Dev,Off,...")

    @Column(nullable = false)
    private String description;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean isDefault = false;

    // Helper method to get hourly activities as array
    public String[] getHourlyActivities() {
        if (hourlyCsv == null || hourlyCsv.isEmpty()) {
            String[] defaults = new String[24];
            for (int i = 0; i < 24; i++) {
                defaults[i] = "Off";
            }
            return defaults;
        }
        return hourlyCsv.split(",");
    }

    // Helper method to set from array
    public void setHourlyActivities(String[] activities) {
        if (activities == null || activities.length != 24) {
            throw new IllegalArgumentException("Must provide exactly 24 hourly activities");
        }
        this.hourlyCsv = String.join(",", activities);
    }
}

