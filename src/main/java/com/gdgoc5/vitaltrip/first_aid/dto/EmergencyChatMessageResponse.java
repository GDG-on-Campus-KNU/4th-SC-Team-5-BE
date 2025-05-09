package com.gdgoc5.vitaltrip.first_aid.dto;

import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyChatMessage;

import java.time.LocalDateTime;

public record EmergencyChatMessageResponse(
    String sender,
    String message,
    LocalDateTime createdAt
) {
    public static EmergencyChatMessageResponse from(EmergencyChatMessage message) {
        return new EmergencyChatMessageResponse(
            message.getSender(),
            message.getMessage(),
            message.getCreatedAt()
        );
    }
}
