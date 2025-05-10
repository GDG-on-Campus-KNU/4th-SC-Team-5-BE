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

        WebClient webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> payload = makePromptForGemini(emergencyType, userMessage);

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

            UUID sessionId = UUID.randomUUID();
            EmergencyChatSession session = new EmergencyChatSession();
            session.setId(sessionId);
            session.setEmergencyType(emergencyType);
            session.setCreatedAt(LocalDateTime.now());
            sessionRepository.save(session);

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
            aiMsg.setMessage(content);
            aiMsg.setCreatedAt(LocalDateTime.now());

            messageRepository.save(userMsg);
            messageRepository.save(aiMsg);

            return EmergencyChatAdviceResponse.from(content, recommendedAction, confidence);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 중 오류 발생", e);
            throw new RuntimeException("Gemini 응답 파싱 중 오류 발생", e);
        }
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

        String prompt = "다음은 사용자가 동일한 응급상담 세션 중에 이어서 질문한 내용입니다.\n" +
                "- 응급 상황 유형: " + session.getEmergencyType() + "\n" +
                "- 사용자 메시지: \"" + userMessage + "\"\n" +
                "이 메시지를 기반으로 적절한 응급처치 조언을 제공해주세요. 응답은 반드시 아래 JSON 형식으로 반환해야 합니다.\n" +
                "{\n" +
                "  \"c\": \"조언 텍스트\",\n" +
                "  \"recommendedAction\": \"권장 행동\",\n" +
                "  \"confidence\": 숫자 (0.0 ~ 1.0)\n" +
                "}";

        WebClient webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

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
            aiMsg.setMessage(content);
            aiMsg.setCreatedAt(LocalDateTime.now());

            messageRepository.save(userMsg);
            messageRepository.save(aiMsg);

            return EmergencyChatAdviceResponse.from(content, recommendedAction, confidence);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 중 오류 발생", e);
            throw new RuntimeException("Gemini 응답 파싱 중 오류 발생", e);
        }
    }

    public List<EmergencyManual> getManualByEmergencyType(EmergencyType emergencyType) {
        return manualRepository.findByEmergencyType(emergencyType);
    }

    public List<EmergencyManual> getAllManuals() {
        return manualRepository.findAll();
    }

    private Map<String, Object> makePromptForGemini(String emergencyType, String userMessage) {
        String prompt = "다음 응급 상황에 대해 의학적인 조언을 제공해주세요.\n" +
                "- 응급 상황 유형: " + emergencyType + "\n" +
                "- 사용자 메시지: \"" + userMessage + "\"\n" +
                "응답은 반드시 아래 JSON 형식으로 반환해주세요.\n" +
                "{\n" +
                "  \"c\": \"조언 텍스트\",\n" +
                "  \"recommendedAction\": \"권장 행동\",\n" +
                "  \"confidence\": 숫자 (0.0 ~ 1.0)\n" +
                "}";

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );
        return payload;
    }
}
