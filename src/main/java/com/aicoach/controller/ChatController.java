package com.aicoach.controller;

import com.aicoach.service.ChatMessage;
import com.aicoach.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aicoach.security.UserPrincipal;

import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ChatRequest request) {
        if (principal == null || principal.getUserId() == null) {
            return ResponseEntity.status(401).body(new ChatResponse("Please log in again."));
        }
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please enter an interview question."));
        }

        List<ChatMessage> history = request.history() == null
                ? List.of()
                : request.history().stream()
                    .filter(m -> m != null)
                    .map(m -> new ChatMessage(m.role(), m.content()))
                    .toList();

        String reply = chatService.chat(request.message(), history);
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    @PostMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        if (principal == null || principal.getUserId() == null) {
            try { emitter.send(SseEmitter.event().name("error").data("Please log in again.")); emitter.complete(); } catch (Exception ignored) {}
            return emitter;
        }
        if (request == null || request.message() == null || request.message().isBlank()) {
            try { emitter.send(SseEmitter.event().name("error").data("Please enter an interview question.")); emitter.complete(); } catch (Exception ignored) {}
            return emitter;
        }
        List<ChatMessage> history = request.history() == null ? List.of() : request.history().stream()
                .filter(m -> m != null).map(m -> new ChatMessage(m.role(), m.content())).toList();
        chatService.streamChat(request.message(), history,
                chunk -> { try { emitter.send(SseEmitter.event().name("token").data(chunk)); } catch (Exception e) { emitter.completeWithError(e); } },
                error -> { try { emitter.send(SseEmitter.event().name("error").data(error.getMessage() == null ? "AI response failed." : error.getMessage())); } catch (Exception ignored) {} emitter.complete(); },
                emitter::complete);
        return emitter;
    }

    public record ChatRequest(String message, List<ChatMessageRequest> history) {}
    public record ChatMessageRequest(String role, String content) {}
    public record ChatResponse(String reply) {}
}
