package com.gdgoc5.vitaltrip.first_aid;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;

@Service
public class FirstAidService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String getLLMBasedEmergencyAdvice(String prompt) {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();

        String requestBody = "{ \"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}]}] }";

        String response = webClient.post()
                .uri("/v1beta/models/gemini-pro:generateContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("응답 중 오류가 발생했습니다.")
                .block();

        return response;
    }
}
