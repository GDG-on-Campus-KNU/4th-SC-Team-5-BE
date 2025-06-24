package com.gdgoc5.vitaltrip.first_aid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gdgoc5.vitaltrip.custom.SuccessResponse;
import com.gdgoc5.vitaltrip.first_aid.dto.EmergencyManualResponse;
import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.UUID;
import com.gdgoc5.vitaltrip.first_aid.dto.EmergencyChatMessageResponse;
import com.gdgoc5.vitaltrip.first_aid.dto.EmergencyChatAdviceResponse;
import com.gdgoc5.vitaltrip.first_aid.dto.EmergencyChatAdviceRequest;
import com.gdgoc5.vitaltrip.first_aid.dto.EmergencyChatContinueRequest;

@Slf4j
@Tag(name = "First Aid", description = "응급처치 AI 상담 API")
@RestController
@RequestMapping("/first-aid")
public class FirstAidController {

    private final FirstAidService firstAidService;

    public FirstAidController(FirstAidService firstAidService) {
        this.firstAidService = firstAidService;
    }

    @Operation(
        summary = "응급처치 초기 상담",
        description = "응급 상황에 대한 첫 AI 응급조치를 수행하고 세션 ID를 반환합니다.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "성공적으로 상담 정보를 반환함",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "응급처치 초기 응답 예시",
                        value = """
                        {
                          "result": "SUCCESS",
                          "message": "요청이 성공적으로 처리되었습니다.",
                          "data": {
                            "content": "You are experiencing hypothermia. The inability to feel your hands is a serious sign. \\n\\n**Immediate Actions:**\\n\\n*   **Get to a Warm Place:** Find shelter immediately, ideally indoors. If indoors isn't possible, find a protected area out of the wind and rain.\\n*   **Remove Wet Clothing:** Take off any wet clothing as quickly as possible. Wet clothing significantly speeds up heat loss.\\n*   **Warm the Core:** Focus on warming your core body temperature. If available, put on dry, warm clothing, especially layers. Cover your head with a hat and your neck with a scarf.\\n*   **Warm Drinks (If Conscious and Alert):** If you are able to swallow without difficulty, drink warm, non-alcoholic and non-caffeinated beverages like broth or warm juice. Avoid alcohol and caffeine as they can worsen hypothermia.\\n*   **Gentle Warmth:** If possible, apply gentle warmth to your core. You can use warm (not hot) water bottles, or body-to-body contact. Avoid direct heat like heating pads or hot water, as these can cause burns.\\n*   **Do Not Rub or Massage Affected Areas:** Avoid rubbing or massaging your hands, as this can cause tissue damage.\\n\\n**Important:** Seek medical attention immediately. Hypothermia can be life-threatening.",
                            "recommendedAction": "Seek immediate medical attention. Call for emergency services or get to the nearest hospital as quickly as possible.",
                            "confidence": 0.95,
                            "suggestedPhrase": "I'm experiencing hypothermia. I can't feel my hands, and I feel very cold and possibly confused. I need immediate medical assistance to warm up safely."
                          }
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/chat")
    @Retryable(
            value = { JsonProcessingException.class, RuntimeException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SuccessResponse<EmergencyChatAdviceResponse> getEmergencyAidAdvice(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "응급 상황 종류와 사용자 메시지를 담은 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "초기 상담 요청 예시",
                                    value = """
                                    {
                                          "emergencyType": "HYPOTHERMIA",
                                          "userMessage": "I can't feel my hands. It's really cold."
                                    }
                                    """
                            )
                    )
            )
            @RequestBody EmergencyChatAdviceRequest request
    ) {
        String emergencyType = request.emergencyType();
        String userMessage = request.userMessage();
        return SuccessResponse.of(firstAidService.getEmergencyChatAdvice(emergencyType, userMessage));
    }

    @Operation(
        summary = "기존 세션의 전체 대화 조회",
        description = "세션 ID를 기반으로 기존의 응급처치 상담 대화를 전부 조회합니다.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "기존 대화 리스트 반환",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "대화 목록 예시",
                        value = """
                        {
                              "result": "SUCCESS",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": [
                                {
                                  "sender": "USER",
                                  "message": "It's so cold I can't feel my hands.",
                                  "createdAt": "2025-05-13T14:51:36.654435"
                                },
                                {
                                  "sender": "ASSISTANT",
                                  "message": "You are experiencing hypothermia. The inability to feel your hands is a serious sign. \\n\\n**Immediate Actions:**\\n\\n*   **Get to a Warm Place:** Find shelter immediately, ideally indoors. If indoors isn't possible, find a protected area out of the wind and rain.\\n*   **Remove Wet Clothing:** Take off any wet clothing as quickly as possible. Wet clothing significantly speeds up heat loss.\\n*   **Warm the Core:** Focus on warming your core body temperature. If available, put on dry, warm clothing, especially layers. Cover your head with a hat and your neck with a scarf.\\n*   **Warm Drinks (If Conscious and Alert):** If you are able to swallow without difficulty, drink warm, non-alcoholic and non-caffeinated beverages like broth or warm juice. Avoid alcohol and caffeine as they can worsen hypothermia.\\n*   **Gentle Warmth:** If possible, apply gentle warmth to your core. You can use warm (not hot) water bottles, or body-to-body contact. Avoid direct heat like heating pads or hot water, as these can cause burns.\\n*   **Do Not Rub or Massage Affected Areas:** Avoid rubbing or massaging your hands, as this can cause tissue damage.\\n\\n**Important:** Seek medical attention immediately. Hypothermia can be life-threatening.",
                                  "createdAt": "2025-05-13T14:51:36.65448"
                                },
                                {
                                  "sender": "USER",
                                  "message": "In this case, which hospital nearby should I go to?",
                                  "createdAt": "2025-05-13T14:52:21.908893"
                                },
                                {
                                  "sender": "ASSISTANT",
                                  "message": "Since you are asking for the nearest hospital, your condition likely requires immediate medical attention. Continue any warming measures you are able to safely administer (remove wet clothing, add dry layers, seek shelter if not already there), but prioritize getting to a hospital. Use your phone to search for the closest emergency room (ER) or dial your local emergency number (like 911 in the US) for immediate transport. Don't delay transport to find the 'best' hospital; the closest one is the priority right now.",
                                  "createdAt": "2025-05-13T14:52:21.908907"
                                }
                              ]
                        }
                        """
                    )
                )
            )
        }
    )
    @GetMapping("/chat/{sessionId}")
    public SuccessResponse<List<EmergencyChatMessageResponse>> getChatMessages(@PathVariable UUID sessionId) {
        return SuccessResponse.of(firstAidService.getChatMessagesBySessionId(sessionId));
    }

    @Operation(
        summary = "응급처치 상담 계속하기",
        description = "세션 ID를 사용하여 기존 응급처치 상담을 이어갑니다.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "상담 응답 반환",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "상담 계속 응답 예시",
                        value = """
                        {
                          "result": "SUCCESS",
                          "message": "요청이 성공적으로 처리되었습니다.",
                          "data": {
                            "content": "Since you believe you are experiencing hypothermia, it is vital to seek immediate medical attention. I cannot provide specific hospital recommendations without knowing your location. Please provide your current location (city, address, or nearby landmark) so I can identify the nearest hospital with emergency services. While waiting for transport, continue warming measures like removing any wet clothing and covering yourself with dry blankets or clothing. Avoid rubbing the affected areas, as this can cause tissue damage. If possible, drink something warm and sweet if you are conscious and able to swallow safely.",
                            "recommendedAction": "Provide location for nearest hospital search and continue warming measures while awaiting transport.",
                            "confidence": 0.95,
                            "blogLinks": [
                              "https://www.mayoclinic.org/first-aid/first-aid-hypothermia/basics/art-20056681",
                              "https://www.redcross.org/get-help/how-to-prepare-for-emergencies/types-of-emergencies/winter-storm/hypothermia.html"
                            ],
                            "sessionId": null
                          }
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/chat/{sessionId}")
    @Retryable(
            value = { JsonProcessingException.class, RuntimeException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SuccessResponse<EmergencyChatAdviceResponse> continueEmergencyChat(
            @PathVariable UUID sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자의 추가 질문",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "상담 계속 요청 예시",
                                    value = """
                                    {
                                      "userMessage": "In this case, which hospital nearby should I go to?"
                                    }
                                    """
                            )
                    )
            )
            @RequestBody EmergencyChatContinueRequest request
    ) {
        String userMessage = request.userMessage();
        return SuccessResponse.of(firstAidService.continueEmergencyChat(sessionId, userMessage));
    }

    @Operation(
            summary = "응급처치 매뉴얼 조회",
            description = "응급상황 유형에 따른 매뉴얼 정보를 조회합니다.",
            parameters = {
                    @Parameter(
                            name = "emergencyType",
                            description = "응급처치 유형 (예: BURNS, CHOKING, FRACTURE 등)",
                            example = "CHOKING"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "응급처치 매뉴얼 반환",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "매뉴얼 응답 예시",
                                            value = """
                                            {
                                              "result": "SUCCESS",
                                              "message": "요청이 성공적으로 처리되었습니다.",
                                              "data": [
                                                {
                                                  "emergencyType": "BURNS",
                                                  "title": "Burns from Hot Water",
                                                  "description": "Basic first aid for burns caused by hot water.",
                                                  "steps": "1. Cool the burned area under running cold water for 10-15 minutes.\\n2. Cover with a clean gauze.\\n3. Consider visiting a hospital if the pain is severe.",
                                                  "warning": "Do not apply ice directly or rub the skin.",
                                                  "updatedAt": "2025-05-13T05:57:58"
                                                },
                                                {
                                                  "emergencyType": "BURNS",
                                                  "title": "Chemical Burns",
                                                  "description": "First aid for burns caused by chemical substances.",
                                                  "steps": "1. Immediately rinse the affected area with running water for more than 20 minutes.\\n2. Remove contaminated clothing.\\n3. Seek medical attention immediately if pain persists or if the eyes are exposed.",
                                                  "warning": "Do not rub your eyes during rinsing.",
                                                  "updatedAt": "2025-05-13T05:57:58"
                                                },
                                                {
                                                  "emergencyType": "BURNS",
                                                  "title": "Electrical Burns",
                                                  "description": "First aid for burns caused by electrical accidents.",
                                                  "steps": "1. Disconnect the power source before approaching.\\n2. Prepare for CPR in case of cardiac arrest.\\n3. Do not cool the burn with water; prioritize transporting the patient to the hospital.",
                                                  "warning": "Do not touch the patient while electricity is still active.",
                                                  "updatedAt": "2025-05-13T05:57:58"
                                                }
                                              ]
                                            }
                                            """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/manuals/{emergencyType}")
    public SuccessResponse<List<EmergencyManualResponse>> getManualByType(@PathVariable EmergencyType emergencyType) {
        return SuccessResponse.of(firstAidService.getManualByEmergencyType(emergencyType)
                .stream().map(EmergencyManualResponse::from).toList());
    }

    @Operation(
        summary = "전체 응급처치 매뉴얼 목록 조회",
        description = "모든 응급상황 유형에 대한 응급처치 매뉴얼 정보를 리스트로 반환합니다.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "응급처치 매뉴얼 목록",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "매뉴얼 목록 예시",
                        value = """
                        {
                          "result": "SUCCESS",
                          "message": "요청이 성공적으로 처리되었습니다.",
                          "data": [
                            {
                              "emergencyType": "BURNS",
                              "title": "Burns from Hot Water",
                              "description": "Basic first aid for burns caused by hot water.",
                              "steps": "1. Cool the burned area under running cold water for 10-15 minutes.\\n2. Cover with a clean gauze.\\n3. Consider visiting a hospital if the pain is severe.",
                              "warning": "Do not apply ice directly or rub the skin.",
                              "updatedAt": "2025-05-13T05:57:58"
                            },
                            {
                              "emergencyType": "SEIZURE",
                              "title": "Partial Seizure",
                              "description": "First aid for partial seizures.",
                              "steps": "1. Move the patient to a quiet place and let them rest.\\n2. Monitor until full recovery of consciousness.",
                              "warning": "Do not overwhelm the patient with questions after awakening.",
                              "updatedAt": "2025-05-13T05:57:58"
                            }
                          ]
                        }
                        """
                    )
                )
            )
        }
    )
    @GetMapping("/manuals")
    public SuccessResponse<List<EmergencyManualResponse>> getAllManuals() {
        return SuccessResponse.of(firstAidService.getAllManuals()
                .stream()
                .map(EmergencyManualResponse::from)
                .toList());
    }


}
