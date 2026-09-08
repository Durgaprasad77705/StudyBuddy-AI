package com.aicoach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean(name = "ollamaRestTemplate")
    public RestTemplate ollamaRestTemplate(AiProperties aiProperties) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        int timeout = (int) aiProperties.getTimeoutMs();

        factory.setConnectTimeout(
                timeout > 0 ? timeout : 20000
        );

        factory.setReadTimeout(
                timeout > 0 ? timeout : 20000
        );

        return new RestTemplate(factory);
    }
}