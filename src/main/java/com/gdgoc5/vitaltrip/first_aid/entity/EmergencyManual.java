package com.gdgoc5.vitaltrip.first_aid.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EmergencyManual {
    @Id
    private UUID id;

    private String emergencyType;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @Column(columnDefinition = "TEXT")
    private String warning;

    private LocalDateTime updatedAt;
}

