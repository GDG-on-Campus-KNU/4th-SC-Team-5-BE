package com.gdgoc5.vitaltrip.first_aid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyChatMessage;
import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyChatSession;
import com.gdgoc5.vitaltrip.first_aid.repository.EmergencyChatMessageRepository;
import com.gdgoc5.vitaltrip.first_aid.repository.EmergencyChatSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
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

    @Autowired
    public FirstAidService(EmergencyChatSessionRepository sessionRepository,
                           EmergencyChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    public Map<String, Object> getEmergencyChatAdvice(String emergencyType, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            Map<String, Object> failResponse = new HashMap<>();
            failResponse.put("message", "userMessage는 필수 입력값입니다.");
            failResponse.put("data", null);
            failResponse.put("result", "FAIL");
            return failResponse;
        }

        WebClient webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();

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
            "contents", java.util.List.of(
                Map.of("parts", java.util.List.of(Map.of("text", prompt)))
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
        log.info("response: {}", response);
        Map<String, Object> result = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            // Gemini API error check
            if (root.has("error")) {
                log.error("Gemini API 응답 오류: {}", root.get("error"));
                result.put("message", "Gemini API 호출 중 오류가 발생했습니다.");
                result.put("data", null);
                result.put("result", "FAIL");
                return result;
            }

            String jsonString = root.at("/candidates/0/content/parts/0/text").asText().trim();
            if (jsonString.startsWith("```json")) {
                jsonString = jsonString.replaceFirst("```json", "").trim();
            }
            if (jsonString.endsWith("```")) {
                jsonString = jsonString.substring(0, jsonString.lastIndexOf("```")).trim();
            }
            log.info(jsonString);

            JsonNode parsed = mapper.readTree(jsonString);

            Map<String, Object> data = new HashMap<>();
            data.put("c", parsed.get("c").asText());
            data.put("recommendedAction", parsed.get("recommendedAction").asText());
            data.put("confidence", parsed.get("confidence").asDouble());

            UUID sessionId = UUID.randomUUID();
            EmergencyChatSession session = new EmergencyChatSession();
            session.setId(sessionId);
            session.setEmergencyType(emergencyType);
            session.setCreatedAt(LocalDateTime.now());
            sessionRepository.save(session);

            data.put("sessionId", sessionId.toString());

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
            aiMsg.setMessage(parsed.get("c").asText());
            aiMsg.setCreatedAt(LocalDateTime.now());

            messageRepository.save(userMsg);
            messageRepository.save(aiMsg);

            result.put("message", "요청이 성공적으로 처리되었습니다.");
            result.put("data", data);
            result.put("result", "SUCCESS");
        } catch (Exception e) {
            result.put("message", "Gemini 응답 파싱 중 오류 발생");
            log.error(e.getMessage(), e);
            result.put("data", null);
            result.put("result", "FAIL");
        }

        return result;
    }

    public List<Map<String, Object>> getChatMessagesBySessionId(UUID sessionId) {
        List<EmergencyChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (EmergencyChatMessage msg : messages) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("sender", msg.getSender());
            entry.put("message", msg.getMessage());
            entry.put("createdAt", msg.getCreatedAt());
            result.add(entry);
        }

        return result;
    }

    public Map<String, Object> continueEmergencyChat(UUID sessionId, String userMessage) {
        Map<String, Object> result = new HashMap<>();

        if (userMessage == null || userMessage.trim().isEmpty()) {
            result.put("message", "userMessage는 필수 입력값입니다.");
            result.put("data", null);
            result.put("result", "FAIL");
            return result;
        }

        EmergencyChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            result.put("message", "해당 세션을 찾을 수 없습니다.");
            result.put("data", null);
            result.put("result", "FAIL");
            return result;
        }

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
                result.put("message", "Gemini API 호출 중 오류가 발생했습니다.");
                result.put("data", null);
                result.put("result", "FAIL");
                return result;
            }

            String jsonString = root.at("/candidates/0/content/parts/0/text").asText().trim();
            if (jsonString.startsWith("```json")) {
                jsonString = jsonString.replaceFirst("```json", "").trim();
            }
            if (jsonString.endsWith("```")) {
                jsonString = jsonString.substring(0, jsonString.lastIndexOf("```")).trim();
            }

            JsonNode parsed = mapper.readTree(jsonString);

            Map<String, Object> data = new HashMap<>();
            data.put("c", parsed.get("c").asText());
            data.put("recommendedAction", parsed.get("recommendedAction").asText());
            data.put("confidence", parsed.get("confidence").asDouble());

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
            aiMsg.setMessage(parsed.get("c").asText());
            aiMsg.setCreatedAt(LocalDateTime.now());

            messageRepository.save(userMsg);
            messageRepository.save(aiMsg);

            result.put("message", "요청이 성공적으로 처리되었습니다.");
            result.put("data", data);
            result.put("result", "SUCCESS");
        } catch (Exception e) {
            result.put("message", "Gemini 응답 파싱 중 오류 발생");
            result.put("data", null);
            result.put("result", "FAIL");
            log.error(e.getMessage(), e);
        }

        return result;
    }


}
