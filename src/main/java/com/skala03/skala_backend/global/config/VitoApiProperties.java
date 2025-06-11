
package com.skala03.skala_backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vito.api")
public class VitoApiProperties {
    private String baseUrl;
    private String clientId;
    private String clientSecret;
}