package com.aicoach.service.impl;

import com.aicoach.config.AiProperties;
import com.aicoach.service.ChatMessage;
import com.aicoach.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public ChatServiceImpl(
            @Qualifier("ollamaRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
    }

    @Override
    public String chat(String message) {
        return chat(message, List.of());
    }

    @Override
    public String chat(String message, List<ChatMessage> history) {
        if (message == null || message.isBlank()) {
            return "Please enter an interview question.";
        }

        if ("anthropic".equalsIgnoreCase(aiProperties.getProvider())
                && aiProperties.getApiKey() != null
                && !aiProperties.getApiKey().isBlank()) {
            try {
                return callAnthropicFast(message, history);
            } catch (Exception e) {
                log.warn("Fast Anthropic chat failed: {}", e.getMessage());
            }
        }

        try {
            return callOllama(message, history);
        } catch (Exception e) {
            log.warn("Local Ollama chat failed: {}", e.getMessage());
            return fallbackAnswer(message);
        }
    }


    @Override
    public void streamChat(String message, List<ChatMessage> history, Consumer<String> onChunk,
                           Consumer<Throwable> onError, Runnable onComplete) {
        CompletableFuture.runAsync(() -> {
            try {
                if (message == null || message.isBlank()) {
                    onChunk.accept("Please enter an interview question.");
                    onComplete.run();
                    return;
                }
                if ("anthropic".equalsIgnoreCase(aiProperties.getProvider())
                        && aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank()) {
                    streamAnthropic(message, history, onChunk);
                } else {
                    streamOllama(message, history, onChunk);
                }
                onComplete.run();
            } catch (Throwable first) {
                log.warn("Streaming AI chat failed: {}", first.getMessage());
                try {
                    String fallback = chat(message, history);
                    onChunk.accept(fallback);
                    onComplete.run();
                } catch (Throwable second) {
                    onError.accept(second);
                }
            }
        });
    }

    private void streamAnthropic(String message, List<ChatMessage> history, Consumer<String> onChunk) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        String model = aiProperties.getFastModel();
        if (model == null || model.isBlank()) model = "claude-haiku-4-5-20251001";
        body.put("model", model);
        body.put("max_tokens", 420);
        body.put("stream", true);
        body.put("system", "You are a fast Java interview coach. Answer naturally and concisely. Focus on Java, Spring Boot, SQL, DSA and interview preparation. Do not reveal internal reasoning or mention the AI provider.");
        ArrayNode messages = body.putArray("messages");
        addHistory(messages, history);
        ObjectNode current = messages.addObject();
        current.put("role", "user");
        current.put("content", message);

        HttpRequest request = HttpRequest.newBuilder(URI.create(aiProperties.getBaseUrl()))
                .header("Content-Type", "application/json")
                .header("x-api-key", aiProperties.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<java.io.InputStream> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 300) throw new IllegalStateException("AI provider returned HTTP " + response.statusCode());
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isBlank() || "[DONE]".equals(data)) continue;
                JsonNode event = objectMapper.readTree(data);
                String delta = event.path("delta").path("text").asText("");
                if (!delta.isBlank()) onChunk.accept(delta);
            }
        }
    }

    private void streamOllama(String message, List<ChatMessage> history, Consumer<String> onChunk) throws Exception {
        String url = aiProperties.getBaseUrl();
        if (url == null || url.isBlank() || !url.contains("11434")) url = "http://127.0.0.1:11434/api/chat";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "llama3.2:3b");
        body.put("stream", true);
        ObjectNode options = body.putObject("options");
        options.put("num_predict", 260);
        ArrayNode messages = body.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", "You are a concise Java interview coach. Answer naturally and clearly.");
        addHistory(messages, history);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", message);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<java.io.InputStream> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 300) throw new IllegalStateException("Ollama returned HTTP " + response.statusCode());
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                JsonNode event = objectMapper.readTree(line);
                String delta = event.path("message").path("content").asText("");
                if (!delta.isBlank()) onChunk.accept(delta);
                if (event.path("done").asBoolean(false)) break;
            }
        }
    }

    private void addHistory(ArrayNode messages, List<ChatMessage> history) {
        if (history == null) return;
        history.stream().filter(h -> h != null && h.content() != null && !h.content().isBlank())
                .skip(Math.max(0, history.size() - 8)).forEach(h -> {
                    ObjectNode m = messages.addObject();
                    m.put("role", "assistant".equalsIgnoreCase(h.role()) ? "assistant" : "user");
                    m.put("content", h.content());
                });
    }

    private String fallbackAnswer(String message) {
        String q = message == null ? "" : message.toLowerCase();
        if (q.contains("oops") || q.contains("object oriented")) {
            return "OOP in Java is based on encapsulation, inheritance, polymorphism, and abstraction. Encapsulation keeps data and behavior together, inheritance enables reuse, polymorphism allows one interface with multiple implementations, and abstraction hides implementation details.";
        }
        if (q.contains("spring boot")) {
            return "Spring Boot simplifies Java application development with auto-configuration, starter dependencies, embedded servers, and production-ready features. Common interview areas include dependency injection, REST controllers, JPA, validation, exception handling, and Spring Security.";
        }
        if (q.contains("exception")) {
            return "Java exceptions represent abnormal conditions during program execution. Checked exceptions are verified by the compiler, while unchecked exceptions extend RuntimeException. Use specific exceptions, handle them at the right layer, and avoid swallowing errors.";
        }
        return "I can help with Java, OOP, Collections, exceptions, multithreading, Spring Boot, REST APIs, JPA/Hibernate, SQL, DBMS, DSA, and interview preparation. Please ask a specific question and I will give you a concise interview-ready answer.";
    }

    private String callAnthropicFast(String message, List<ChatMessage> history) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", aiProperties.getApiKey());
        headers.set("anthropic-version", "2023-06-01");

        ObjectNode body = objectMapper.createObjectNode();
        String model = aiProperties.getFastModel();
        if (model == null || model.isBlank()) model = "claude-haiku-4-5-20251001";
        body.put("model", model);
        body.put("max_tokens", 320);
        body.put("system", "You are a fast Java interview coach. Answer concisely in 4-8 sentences. Focus on Java, OOP, Collections, exceptions, multithreading, Java 8+, Spring Boot, REST, JPA/Hibernate, SQL, DBMS and DSA. Give a short example when useful. Do not reveal internal reasoning or say you are using a specific provider.");

        ArrayNode messages = body.putArray("messages");
        if (history != null && !history.isEmpty()) {
            history.stream()
                    .filter(h -> h != null && h.content() != null && !h.content().isBlank())
                    .skip(Math.max(0, history.size() - 6))
                    .forEach(h -> {
                        ObjectNode m = messages.addObject();
                        m.put("role", "assistant".equalsIgnoreCase(h.role()) ? "assistant" : "user");
                        m.put("content", h.content());
                    });
        }

        ObjectNode current = messages.addObject();
        current.put("role", "user");
        current.put("content", message);

        JsonNode response = restTemplate.postForObject(
                aiProperties.getBaseUrl(),
                new HttpEntity<>(body.toString(), headers),
                JsonNode.class);

        String answer = response == null ? "" : response.path("content").path(0).path("text").asText("");
        if (answer.isBlank()) throw new IllegalStateException("Empty AI response");
        return answer.trim();
    }

    private String callOllama(String message, List<ChatMessage> history) throws Exception {
        String url = aiProperties.getBaseUrl();
        if (url == null || url.isBlank() || !url.contains("11434")) {
            url = "http://127.0.0.1:11434/api/chat";
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "llama3.2:3b");
        body.put("stream", false);

        ObjectNode options = objectMapper.createObjectNode();
        options.put("num_predict", 220);
        body.set("options", options);

        ArrayNode messages = body.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", "You are a concise Java interview coach. Answer in 4-8 sentences.");

        if (history != null && !history.isEmpty()) {
            history.stream()
                    .filter(h -> h != null && h.content() != null && !h.content().isBlank())
                    .skip(Math.max(0, history.size() - 6))
                    .forEach(h -> {
                        ObjectNode m = messages.addObject();
                        m.put("role", "assistant".equalsIgnoreCase(h.role()) ? "assistant" : "user");
                        m.put("content", h.content());
                    });
        }

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JsonNode response = restTemplate.postForObject(
                url,
                new HttpEntity<>(body.toString(), headers),
                JsonNode.class);

        String answer = response == null ? "" : response.path("message").path("content").asText("");
        if (answer.isBlank()) throw new IllegalStateException("Empty Ollama response");
        return answer.trim();
    }
}
