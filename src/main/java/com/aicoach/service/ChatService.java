package com.aicoach.service;

import java.util.List;
import java.util.function.Consumer;

public interface ChatService {
    String chat(String message);
    String chat(String message, List<ChatMessage> history);

    void streamChat(String message, List<ChatMessage> history, Consumer<String> onChunk, Consumer<Throwable> onError, Runnable onComplete);
}
