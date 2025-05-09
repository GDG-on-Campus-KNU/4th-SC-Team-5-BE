package com.gdgoc5.vitaltrip.first_aid;

import com.fasterxml.jackson.core.JsonProcessingException;
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
                           "data": {
                             "recommendedAction": "가능한 한 빨리 병원 또는 화상 전문 병원을 방문하여 진료를 받으십시오. 냉찜질로 통증을 완화하고, 깨끗한 거즈로 화상 부위를 덮어 보호하면서 병원으로 이동하십시오. 물집이 생겼다면 터뜨리지 마십시오.",
                             "c": "화상 부위가 붓고 통증이 심하다면 2도 화상 이상일 가능성이 있습니다. 2도 화상은 물집이 생기거나 피부가 벗겨질 수 있으며, 통증이 심합니다. 감염의 위험도 있으므로 병원 방문이 필요할 수 있습니다. 특히 화상 부위가 넓거나, 얼굴/손/발/생식기 부위에 화상을 입은 경우, 또는 화상으로 인해 호흡 곤란, 쇼크 등의 증상이 나타나는 경우에는 즉시 응급실로 가셔야 합니다.",
                             "confidence": 0.95,
                             "sessionId": "38031983-062a-4853-be5b-87c15735b9df"
                           },
                           "message": "요청이 성공적으로 처리되었습니다."
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
    public EmergencyChatAdviceResponse getEmergencyAidAdvice(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "응급 상황 종류와 사용자 메시지를 담은 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "초기 상담 요청 예시",
                                    value = """
                                    {
                                      "emergencyType": "BURNS",
                                      "userMessage": "화상 부위가 부어오르고 통증이 심해요. 병원 가야 하나요?"
                                    }
                                            
                                    """
                            )
                    )
            )
            @RequestBody EmergencyChatAdviceRequest request
    ) {
        String emergencyType = request.emergencyType();
        String userMessage = request.userMessage();
        return firstAidService.getEmergencyChatAdvice(emergencyType, userMessage);
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
                        [
                            {
                              "createdAt": "2025-05-09T15:14:15.377152",
                              "sender": "USER",
                              "message": "화상 부위가 부어오르고 통증이 심해요. 병원 가야 하나요?"
                            },
                            {
                              "createdAt": "2025-05-09T15:14:15.377202",
                              "sender": "ASSISTANT",
                              "message": "화상 부위가 부어오르고 통증이 심하다면 2도 이상의 화상일 가능성이 있습니다. 2도 화상은 물집이 생기거나 피부가 벗겨질 수 있으며 심한 통증을 동반합니다. 넓은 부위에 화상을 입었거나, 얼굴/손/발/생식기 부위에 화상을 입었을 경우, 또는 호흡 곤란이나 다른 증상이 동반된다면 즉시 병원에 가야 합니다. 집에서 응급처치를 할 수 있는 범위가 아닙니다. "
                            }
                          ]
                        """
                    )
                )
            )
        }
    )
    @GetMapping("/chat/{sessionId}")
    public List<EmergencyChatMessageResponse> getChatMessages(@PathVariable UUID sessionId) {
        return firstAidService.getChatMessagesBySessionId(sessionId);
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
                          "data": {
                            "recommendedAction": "상처 세척, 항생 연고 도포, 멸균 거즈 드레싱",
                            "c": "물집이 터진 경우, 감염을 예방하는 것이 중요합니다. 흐르는 깨끗한 물(수돗물)로 조심스럽게 상처 부위를 씻어내고, 항생 연고(네오스포린 등)를 얇게 바르세요. 그 후, 멸균 거즈로 덮고 느슨하게 드레싱하세요. 드레싱은 매일 갈아주는 것이 좋습니다. 만약 감염 징후(붉어짐, 부어오름, 통증 증가, 고름 발생)가 보이면 즉시 의사의 진료를 받으십시오.",
                            "confidence": 0.95
                          },
                          "message": "요청이 성공적으로 처리되었습니다."
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
    public EmergencyChatAdviceResponse continueEmergencyChat(
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
                                      "userMessage": "물집이 터졌는데 어떻게 해야 하나요?"
                                    }
                                    """
                            )
                    )
            )
            @RequestBody EmergencyChatContinueRequest request
    ) {
        String userMessage = request.userMessage();
        return firstAidService.continueEmergencyChat(sessionId, userMessage);
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
                                               "emergencyType": "CHOKING",
                                               "title": "기도막힘 응급처치 매뉴얼",
                                               "description": "기도가 막힌 사람을 구하기 위한 조치입니다.",
                                               "steps": "1. 기침 유도\\n2. 하임리히법 시행 (복부 밀치기)\\n3. 의식 없으면 CPR 시행",
                                               "warning": "등을 세게 두드리는 행위는 효과가 제한적일 수 있습니다.",
                                               "updatedAt": "2025-05-09T13:02:54"
                                             }
                        """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/manuals/{emergencyType}")
    public EmergencyManualResponse getManualByType(@PathVariable EmergencyType emergencyType) {
        return EmergencyManualResponse.from(firstAidService.getManualByEmergencyType(emergencyType));
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
                        [
                          {
                            "emergencyType": "BURNS",
                            "title": "화상 응급처치 매뉴얼",
                            "description": "화상이 발생했을 때의 기본 응급처치 절차입니다.",
                            "steps": "1. 흐르는 시원한 물로 10분 이상 식히기\\n2. 깨끗한 거즈로 부위 감싸기\\n3. 연고 및 얼음 금지",
                            "warning": "물집을 터뜨리거나 얼음을 직접 대지 마세요.",
                            "updatedAt": "2025-05-09T13:00:00"
                          },
                          {
                            "emergencyType": "CHOKING",
                            "title": "기도막힘 응급처치 매뉴얼",
                            "description": "기도가 막힌 사람을 구하기 위한 조치입니다.",
                            "steps": "1. 기침 유도\\n2. 하임리히법 시행 (복부 밀치기)\\n3. 의식 없으면 CPR 시행",
                            "warning": "등을 세게 두드리는 행위는 효과가 제한적일 수 있습니다.",
                            "updatedAt": "2025-05-09T13:02:54"
                          }
                        ]
                        """
                    )
                )
            )
        }
    )
    @GetMapping("/manuals")
    public List<EmergencyManualResponse> getAllManuals() {
        return firstAidService.getAllManuals()
                .stream()
                .map(EmergencyManualResponse::from)
                .toList();
    }


}
