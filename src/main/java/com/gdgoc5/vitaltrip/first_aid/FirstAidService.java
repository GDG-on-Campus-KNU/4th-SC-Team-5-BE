package com.gdgoc5.vitaltrip.first_aid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgoc5.vitaltrip.first_aid.dto.EmergencyChatAdviceResponse;
import com.gdgoc5.vitaltrip.first_aid.dto.EmergencyChatMessageResponse;
import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyChatMessage;
import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyChatSession;
import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyManual;
import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyType;
import com.gdgoc5.vitaltrip.first_aid.repository.EmergencyChatMessageRepository;
import com.gdgoc5.vitaltrip.first_aid.repository.EmergencyChatSessionRepository;
import com.gdgoc5.vitaltrip.first_aid.repository.EmergencyManualRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
public class FirstAidService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final EmergencyChatSessionRepository sessionRepository;
    private final EmergencyChatMessageRepository messageRepository;
    private final EmergencyManualRepository manualRepository;

    @Autowired
    public FirstAidService(EmergencyChatSessionRepository sessionRepository,
                           EmergencyChatMessageRepository messageRepository,
                           EmergencyManualRepository manualRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.manualRepository = manualRepository;
    }

    public EmergencyChatAdviceResponse getEmergencyChatAdvice(String emergencyType, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("userMessage는 필수 입력값입니다.");
        }

        Map<String, Object> payload = makeEmergencyPrompt(emergencyType, userMessage, false);
        EmergencyChatAdviceResponse advice = callGeminiAndParseResponse(payload);

        UUID sessionId = UUID.randomUUID();
        EmergencyChatSession session = new EmergencyChatSession();
        session.setId(sessionId);
        session.setEmergencyType(emergencyType);
        session.setCreatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        return getEmergencyChatAdviceResponse(userMessage, advice, session);
    }

    private EmergencyChatAdviceResponse getEmergencyChatAdviceResponse(String userMessage, EmergencyChatAdviceResponse advice, EmergencyChatSession session) {
        EmergencyChatMessage userMsg = new EmergencyChatMessage();
        userMsg.setId(UUID.randomUUID());
        userMsg.setSession(session);
        userMsg.setSender("USER");
        userMsg.setMessage(userMessage);
        userMsg.setCreatedAt(LocalDateTime.now());

        EmergencyChatMessage aiMsg = new EmergencyChatMessage();
        aiMsg.setId(UUID.randomUUID());
        aiMsg.setSession(session);
        aiMsg.setSender("ASSISTANT");
        aiMsg.setMessage(advice.content());
        aiMsg.setCreatedAt(LocalDateTime.now());

        messageRepository.save(userMsg);
        messageRepository.save(aiMsg);

        return advice;
    }

    public List<EmergencyChatMessageResponse> getChatMessagesBySessionId(UUID sessionId) {
        List<EmergencyChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(EmergencyChatMessageResponse::from)
                .toList();
    }

    public EmergencyChatAdviceResponse continueEmergencyChat(UUID sessionId, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("userMessage는 필수 입력값입니다.");
        }

        EmergencyChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 세션을 찾을 수 없습니다."));

        Map<String, Object> payload = makeEmergencyPrompt(session.getEmergencyType(), userMessage, true);
        EmergencyChatAdviceResponse advice = callGeminiAndParseResponse(payload);

        return getEmergencyChatAdviceResponse(userMessage, advice, session);
    }

    public List<EmergencyManual> getManualByEmergencyType(EmergencyType emergencyType) {
        return manualRepository.findByEmergencyType(emergencyType);
    }

    public List<EmergencyManual> getAllManuals() {
        return manualRepository.findAll();
    }

    private Map<String, Object> makeEmergencyPrompt(String emergencyType, String userMessage, boolean isFollowUp) {
        String intro = isFollowUp
                ? "The following is a follow-up message from the user during the ongoing emergency consultation session."
                : "Please provide first aid advice for the following emergency situation.";

        String prompt = intro + "\n" +
                "- Emergency Type: " + emergencyType + "\n" +
                "- User Message: \"" + userMessage + "\"\n" +
                "Based on this information, provide first aid advice without using markdown formatting like **bold**.\n" +
                "Additionally, suggest two blog article links that explain self-care methods related to the symptoms.\n" +
                "The response must strictly follow the JSON format below:\n" +
                "{\n" +
                "  \"c\": \"Advice text\",\n" +
                "  \"recommendedAction\": \"Recommended action\",\n" +
                "  \"confidence\": number (0.0 ~ 1.0),\n" +
                "  \"blogLinks\": [\"link1\", \"link2\"]\n" +
                "}";

        return Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );
    }

    private EmergencyChatAdviceResponse callGeminiAndParseResponse(Map<String, Object> payload) {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();

        String response = webClient.post()
                .uri("/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("{\"error\":\"Gemini API 오류\"}")
                .block();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            if (root.has("error")) {
                log.error("Gemini API 응답 오류: {}", root.get("error"));
                throw new RuntimeException("Gemini API 호출 중 오류가 발생했습니다.");
            }

            String jsonString = root.at("/candidates/0/content/parts/0/text").asText().trim();
            log.debug("jsonString: {}", jsonString);
            if (jsonString.startsWith("```json")) {
                jsonString = jsonString.replaceFirst("```json", "").trim();
            }
            if (jsonString.endsWith("```")) {
                jsonString = jsonString.substring(0, jsonString.lastIndexOf("```")).trim();
            }

            JsonNode parsed = mapper.readTree(jsonString);
            String content = parsed.get("c").asText();
            String recommendedAction = parsed.get("recommendedAction").asText();
            double confidence = parsed.get("confidence").asDouble();

            List<String> blogLinks = new ArrayList<>();
            parsed.get("blogLinks").forEach(node -> blogLinks.add(node.asText()));

            return EmergencyChatAdviceResponse.from(content, recommendedAction, confidence, blogLinks);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 중 오류 발생", e);
            throw new RuntimeException("Gemini 응답 파싱 중 오류 발생", e);
        }
    }
}
