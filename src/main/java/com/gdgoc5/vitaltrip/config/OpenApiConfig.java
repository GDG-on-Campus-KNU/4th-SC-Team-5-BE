package com.gdgoc5.vitaltrip.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VitalTrip API")
                        .version("v1.0.1")
                        .description("VitalTrip 서비스용 API 문서입니다."));
    }
}
