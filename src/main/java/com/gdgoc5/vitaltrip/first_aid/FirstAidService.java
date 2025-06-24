package com.gdgoc5.vitaltrip.first_aid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgoc5.vitaltrip.exception.NotFoundException;
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
import java.util.logging.Logger;

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
        EmergencyChatAdviceResponse advice = callGeminiAndParseResponse(payload, EmergencyType.valueOf(emergencyType));

        UUID sessionId = UUID.randomUUID();
        EmergencyChatSession session = new EmergencyChatSession();
        session.setId(sessionId);
        session.setEmergencyType(emergencyType);
        session.setCreatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        saveEmergencyChatMessages(userMessage, advice, session);

        // TODO: sessionId를 추가하려고 기존 EmergencyChatAdviceResponse를 새로 생성하는 방식은 비효율적임
        //  초기 상담용 전용 DTO를 별도로 만들어서 sessionId를 포함하는 구조로 개선할 것
        return advice.withSessionId(sessionId);
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
                .orElseThrow(() -> new NotFoundException("해당 세션을 찾을 수 없습니다."));

        Map<String, Object> payload = makeEmergencyPrompt(session.getEmergencyType(), userMessage, true);
        EmergencyChatAdviceResponse advice = callGeminiAndParseResponse(payload, EmergencyType.valueOf(session.getEmergencyType()));

        saveEmergencyChatMessages(userMessage, advice, session);
        return advice;
    }

    public List<EmergencyManual> getManualByEmergencyType(EmergencyType emergencyType) {
        return manualRepository.findByEmergencyType(emergencyType);
    }

    public List<EmergencyManual> getAllManuals() {
        return manualRepository.findAll();
    }

    private void saveEmergencyChatMessages(String userMessage, EmergencyChatAdviceResponse advice, EmergencyChatSession session) {
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
    }



    private double evaluateConfidence(String content, EmergencyType type) {
        List<String> essentialKeywords = switch (type) {
            case BLEEDING -> List.of("pressure", "bleeding", "bandage", "wound");
            case BURNS -> List.of("cool", "running water", "burn", "ointment");
            case FRACTURE -> List.of("immobilize", "fracture", "splint", "swelling");
            case CPR -> List.of("chest compressions", "CPR", "check responsiveness", "call emergency");
            case CHOKING -> List.of("Heimlich", "choking", "cough", "back blows");
            case ELECTRIC_SHOCK -> List.of("electric shock", "unplug", "do not touch", "CPR");
            case HYPOTHERMIA -> List.of("warm", "hypothermia", "blanket", "remove wet clothes");
            case HEATSTROKE -> List.of("cool", "heatstroke", "shade", "hydrate");
            case POISONING -> List.of("poison", "do not induce vomiting", "toxin", "call poison control");
            case SEIZURE -> List.of("seizure", "protect head", "do not restrain", "stay with");
            case ANIMAL_BITE -> List.of("animal bite", "clean wound", "tetanus", "rabies");
            case ASTHMA_ATTACK -> List.of("inhaler", "asthma", "sit upright", "breathe slowly");
            case HEART_ATTACK -> List.of("chest pain", "heart attack", "call emergency", "aspirin");
        };

        long matched = essentialKeywords.stream()
                .filter(word -> content.toLowerCase().contains(word.toLowerCase()))
                .count();

        return Math.max(0.3, (double) matched / essentialKeywords.size());
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
                "The 'confidence' field must be a number between 0.0 and 1.0 indicating how confident you are in the accuracy and reliability of the advice you are providing. Set this value based on your understanding of the situation.\n" +
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

    private EmergencyChatAdviceResponse callGeminiAndParseResponse(Map<String, Object> payload, EmergencyType emergencyType) {
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

            double modelConfidence = parsed.get("confidence").asDouble();
            System.out.println("modelConfidence = " + modelConfidence);
            double evaluatedConfidence = evaluateConfidence(content, emergencyType);
            System.out.println("evaluatedConfidence = " + evaluatedConfidence);
            double finalConfidence = (modelConfidence * 0.7) + (evaluatedConfidence * 0.3);

            List<String> blogLinks = switch (emergencyType) {
                case BLEEDING -> List.of("https://www.webmd.com/first-aid/bleeding-cuts-wounds", "https://www.medicalnewstoday.com/articles/319433");
                case BURNS -> List.of("https://www.nhs.uk/conditions/burns-and-scalds/treatment/", "https://www.mayoclinic.org/first-aid/first-aid-burns/basics/art-20056649");
                case FRACTURE -> List.of("https://my.clevelandclinic.org/health/diseases/15241-bone-fractures", "https://www.betterhealth.vic.gov.au/health/conditionsandtreatments/bone-fractures");
                case CPR -> List.of("https://thecprsolution.net/", "https://www.yourcprsolution.com/");
                case CHOKING -> List.of("https://www.mayoclinic.org/first-aid/first-aid-choking/basics/art-20056637", "https://my.clevelandclinic.org/health/diseases/choking");
                case ELECTRIC_SHOCK -> List.of("https://www.mayoclinic.org/first-aid/first-aid-electrical-shock/basics/art-20056695", "https://www.safetyfirstaid.co.uk/electric-shock-first-aid-treatment/");
                case HYPOTHERMIA -> List.of("https://www.mayoclinic.org/diseases-conditions/hypothermia/diagnosis-treatment/drc-20352688", "https://www.redcross.org/take-a-class/resources/learn-first-aid/hypothermia?srsltid=AfmBOoobkxTEZcuHG-ypoa2XfzZtjrxl4fxqX8yVjMB0V0BUljWv9lZq");
                case HEATSTROKE -> List.of("https://www.mayoclinic.org/first-aid/first-aid-heatstroke/basics/art-20056655", "https://my.clevelandclinic.org/health/diseases/21812-heatstroke");
                case POISONING -> List.of("https://www.mayoclinic.org/first-aid/first-aid-poisoning/basics/art-20056657", "https://www.webmd.com/first-aid/poisoning-treatment");
                case SEIZURE -> List.of("https://www.mayoclinic.org/diseases-conditions/seizure/diagnosis-treatment/drc-20365730", "https://www.healthdirect.gov.au/seizures");
                case ANIMAL_BITE -> List.of("https://www.mayoclinic.org/first-aid/first-aid-animal-bites/basics/art-20056591", "https://medlineplus.gov/ency/patientinstructions/000734.htm");
                case ASTHMA_ATTACK -> List.of("https://www.mayoclinic.org/diseases-conditions/asthma-attack/diagnosis-treatment/drc-20354274", "https://www.healthline.com/health/emergency-home-remedies-for-asthma-attacks");
                case HEART_ATTACK -> List.of("https://my.clevelandclinic.org/health/diseases/16818-heart-attack-myocardial-infarction", "https://www.nhs.uk/conditions/heart-attack/treatment/");
            };


            return EmergencyChatAdviceResponse.from(content, recommendedAction, finalConfidence, blogLinks);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 중 오류 발생", e);
            throw new RuntimeException("Gemini 응답 파싱 중 오류 발생", e);
        }
    }
}
