package com.gdgoc5.vitaltrip.first_aid.dto;

import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyManual;
import java.time.LocalDateTime;

public record EmergencyManualResponse(
        String emergencyType,
        String title,
        String description,
        String steps,
        String warning,
        LocalDateTime updatedAt
) {
    public static EmergencyManualResponse from(EmergencyManual manual) {
        return new EmergencyManualResponse(
                manual.getEmergencyType().name(),
                manual.getTitle(),
                manual.getDescription(),
                manual.getSteps(),
                manual.getWarning(),
                manual.getUpdatedAt()
        );
    }
}
