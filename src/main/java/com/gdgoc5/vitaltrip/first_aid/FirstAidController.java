package com.gdgoc5.vitaltrip.first_aid;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/first-aid")
public class FirstAidController {

    @GetMapping("/chat")
    public String getEmergencyAidAdvice() {
        // TODO: Replace this with actual LLM-based advice logic
        return "긴급 응급처치 상담 결과입니다.";
    }
}
