package com.gdgoc5.vitaltrip.first_aid.dto;

public record EmergencyChatAdviceResponse(
    String content,
    String recommendedAction,
    double confidence,
    String suggestedPhrase
) {
    public static EmergencyChatAdviceResponse from(String content, String recommendedAction, double confidence, String suggestedPhrase) {
        return new EmergencyChatAdviceResponse(content, recommendedAction, confidence, suggestedPhrase);
    }
}
