package com.aicoach.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AiProperties {
    private String provider;
    private String apiKey;
    private String model;
    private String fastModel;
    private String baseUrl;
    private long timeoutMs;
}
