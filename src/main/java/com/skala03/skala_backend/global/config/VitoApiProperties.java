// VitoApiProperties.java - 외부 설정값 바인딩
package com.skala03.skala_backend.global.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vito.api")
public class VitoApiProperties {
    private String clientId;
    private String clientSecret;
    private String baseUrl = "https://openapi.vito.ai";
}
