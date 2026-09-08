package com.aicoach.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaService {

    private final RestTemplate restTemplate;

    private static final String OLLAMA_URL =
            "http://127.0.0.1:11434/api/chat";

    private static final String MODEL =
            "llama3.2:3b";

    public OllamaService() {
        this.restTemplate = new RestTemplate();
    }

    public String ask(String prompt) {

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = new HashMap<>();

        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> request = new HashMap<>();

        request.put("model", MODEL);
        request.put("messages", List.of(message));
        request.put("stream", false);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, headers);

        Map<?, ?> response =
                restTemplate.postForObject(
                        OLLAMA_URL,
                        entity,
                        Map.class
                );

        if (response == null) {
            throw new RuntimeException(
                    "Empty response received from Ollama"
            );
        }

        Object messageObject =
                response.get("message");

        if (!(messageObject instanceof Map)) {
            throw new RuntimeException(
                    "Invalid Ollama response"
            );
        }

        Map<?, ?> responseMessage =
                (Map<?, ?>) messageObject;

        Object content =
                responseMessage.get("content");

        if (content == null) {
            throw new RuntimeException(
                    "Ollama returned no content"
            );
        }

        return content.toString();
    }

    public String askAI(String prompt) {
        return ask(prompt);
    }
}