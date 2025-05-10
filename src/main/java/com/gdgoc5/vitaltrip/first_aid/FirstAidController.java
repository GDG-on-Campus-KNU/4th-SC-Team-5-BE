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
                            "content": "저체온증은 생명을 위협하는 응급 상황입니다. 손에 감각이 없는 것은 저체온증의 초기 징후일 수 있습니다. 즉시 조치를 취해야 합니다. 핵심은 체온을 올리는 것입니다. 하지만 급격하게 체온을 올리는 것은 위험할 수 있으므로 천천히 진행해야 합니다.",
                            "recommendedAction": "1. 즉시 따뜻한 곳으로 이동하세요. 실내로 들어가거나, 바람을 막을 수 있는 곳으로 가세요.\\n2. 젖은 옷을 모두 벗고 마른 옷으로 갈아입으세요. 가능하다면 여러 겹의 옷을 입으세요. 모자, 장갑, 목도리를 착용하여 열 손실을 막으세요.\\n3. 따뜻한 담요 (가능하면) 덮으세요.\\n4. 따뜻한 음료 (설탕이 든 차 또는 스포츠 음료)를 마셔 몸에 열을 공급하고 혈당 수치를 높이세요. 술이나 카페인이 든 음료는 피하세요.\\n5. 피부 대 피부 접촉을 통해 몸을 따뜻하게 하세요. 다른 사람이 마른 옷을 입고 당신을 껴안으면 도움이 될 수 있습니다.\\n6. 움직이세요. 가볍게 팔다리를 움직여 혈액 순환을 촉진하세요.\\n7. 만약 증상이 호전되지 않거나 악화된다면 (예: 심한 떨림, 혼란, 졸음, 발음 불명확 등), 즉시 119에 전화하여 의료 지원을 요청하세요. 의식이 없는 사람에게는 음료를 주지 마세요.",
                            "confidence": 0.95
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
                                      "userMessage": "너무 추워서 손에 감각이 없어요"
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
                                 "message": "화상 부위가 부어오르고 통증이 심해요. 병원 가야 하나요?",
                                 "createdAt": "2025-05-09T22:19:27.114084"
                               },
                               {
                                 "sender": "ASSISTANT",
                                 "message": "화상 부위가 붓고 통증이 심하다면 병원 방문을 고려해야 합니다. 증상이 심한 경우 2도 화상 이상일 가능성이 있으며, 감염 위험도 있습니다. 지금 당장 흐르는 찬물에 화상 부위를 10-20분 정도 식히세요. 물집이 생겼다면 터뜨리지 마시고, 깨끗한 거즈나 천으로 덮어 보호하세요. 통증이 심하다면 진통제를 복용할 수 있지만, 의사 또는 약사와 상담 후 복용하는 것이 좋습니다.",
                                 "createdAt": "2025-05-09T22:19:27.114143"
                               },
                               {
                                 "sender": "USER",
                                 "message": "물집이 터졌는데 어떻게 해야 하나요?",
                                 "createdAt": "2025-05-09T22:20:47.299083"
                               },
                               {
                                 "sender": "ASSISTANT",
                                 "message": "물집이 터졌을 경우 감염을 예방하는 것이 중요합니다. 깨끗한 물과 순한 비누로 조심스럽게 해당 부위를 씻으세요. 절대 문지르지 마세요. 깨끗한 천이나 거즈로 물기를 닦아내고, 항생 연고를 얇게 바르세요. 그 후 멸균 거즈나 반창고로 덮어 보호하세요. 매일 소독하고 새 드레싱으로 교체해야 합니다. 감염 징후(붉어짐, 부어오름, 통증 증가, 고름)가 나타나면 즉시 병원에 방문하세요.",
                                 "createdAt": "2025-05-09T22:20:47.29913"
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
                                      "userMessage": "물집이 터졌는데 어떻게 해야 하나요?"
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
                                                  "title": "뜨거운 물에 의한 화상",
                                                  "description": "뜨거운 물에 데였을 때 기본 응급처치입니다.",
                                                  "steps": "1. 흐르는 찬물로 10~15분간 화상 부위를 식히세요.\\n2. 깨끗한 거즈로 덮으세요.\\n3. 통증이 심하면 병원 방문을 고려하세요.",
                                                  "warning": "얼음을 직접 대거나 피부를 문지르지 마세요.",
                                                  "updatedAt": "2025-05-10T04:13:05"
                                                },
                                                {
                                                  "emergencyType": "BURNS",
                                                  "title": "화학 물질에 의한 화상",
                                                  "description": "화학 약품에 노출되었을 때 응급처치 방법입니다.",
                                                  "steps": "1. 오염 부위를 즉시 흐르는 물로 20분 이상 세척하세요.\\n2. 오염된 옷을 제거하세요.\\n3. 통증이 계속되거나 눈에 노출된 경우 즉시 병원에 가세요.",
                                                  "warning": "세척 중 눈을 비비지 마세요.",
                                                  "updatedAt": "2025-05-10T04:13:05"
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
                               "title": "전기에 의한 화상",
                               "description": "전기 사고로 인한 화상 시 대응 방법입니다.",
                               "steps": "1. 전원 차단 후 접근하세요.\\n2. 심정지 가능성 대비 CPR을 준비하세요.\\n3. 화상 부위는 흐르는 물로 식히지 말고 병원 이송을 우선하세요.",
                               "warning": "전기가 흐르는 상태에서는 환자에게 접촉하지 마세요.",
                               "updatedAt": "2025-05-10T04:13:05"
                             },
                             {
                               "emergencyType": "FRACTURE",
                               "title": "팔 골절",
                               "description": "팔이 부러진 경우의 응급처치 방법입니다.",
                               "steps": "1. 부목이나 단단한 물체로 팔을 고정하세요.\\n2. 움직이지 않게 하고 병원으로 이송하세요.",
                               "warning": "뼈를 억지로 맞추려 하지 마세요.",
                               "updatedAt": "2025-05-10T04:13:05"
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
