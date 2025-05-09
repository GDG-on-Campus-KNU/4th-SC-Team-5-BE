package com.gdgoc5.vitaltrip.first_aid.dto;

public record EmergencyChatAdviceResponse(
    String content,
    String recommendedAction,
    double confidence
) {
    public static EmergencyChatAdviceResponse from(String content, String recommendedAction, double confidence) {
        return new EmergencyChatAdviceResponse(content, recommendedAction, confidence);
    }
}
