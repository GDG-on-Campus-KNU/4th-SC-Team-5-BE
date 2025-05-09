package com.gdgoc5.vitaltrip.first_aid.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
public class EmergencyManual {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private EmergencyType emergencyType;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Lob
    private String steps;

    @Lob
    private String warning;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
