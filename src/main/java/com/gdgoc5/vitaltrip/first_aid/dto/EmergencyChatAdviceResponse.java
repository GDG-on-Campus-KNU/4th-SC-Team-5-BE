package com.gdgoc5.vitaltrip.first_aid.dto;

import java.util.List;
import java.util.UUID;

public record EmergencyChatAdviceResponse(
    String content,
    String recommendedAction,
    double confidence,
    List<String> blogLinks,
    UUID sessionId
) {
    public static EmergencyChatAdviceResponse from(String content, String recommendedAction, double confidence, List<String> blogLinks, UUID sessionId) {
        return new EmergencyChatAdviceResponse(content, recommendedAction, confidence, blogLinks, sessionId);
    }

    public static EmergencyChatAdviceResponse from(String content, String recommendedAction, double confidence, List<String> blogLinks) {
        return new EmergencyChatAdviceResponse(content, recommendedAction, confidence, blogLinks, null);
    }

    /**
     * 이미 생성된 EmergencyChatAdviceResponse 객체에 sessionId를 추가하여 새로운 객체를 반환합니다.
     * 초기 상담 후 세션 ID가 생성되는 경우에 사용됩니다.
     */
    public EmergencyChatAdviceResponse withSessionId(UUID sessionId) {
        return new EmergencyChatAdviceResponse(this.content, this.recommendedAction, this.confidence, this.blogLinks, sessionId);
    }
}
