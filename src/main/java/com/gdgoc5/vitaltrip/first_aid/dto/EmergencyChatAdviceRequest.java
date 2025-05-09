package com.gdgoc5.vitaltrip.first_aid.dto;

public record EmergencyChatAdviceRequest(
        String emergencyType,
        String userMessage
) {}
