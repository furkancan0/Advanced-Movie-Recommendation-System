package com.movieapp.controller;

import com.movieapp.repository.MovieRepository;
import com.movieapp.repository.UserRepository;
import com.movieapp.service.OllamaEmbeddingService;
import com.movieapp.service.OllamaLLMService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OllamaEmbeddingService ollamaService;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final OllamaLLMService llmService;

    /**
     * Test Ollama connection
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testOllamaConnection() {
        boolean connected = ollamaService.testConnection();

        return ResponseEntity.ok(Map.of(
                "connected", connected,
                "message", connected ? "Ollama is running" : "Cannot connect to Ollama",
                "url", "http://localhost:11434"
        ));
    }
    /**
     * Test LLM connection
     */
    @GetMapping("/test-llm")
    public ResponseEntity<Map<String, Object>> testLLM() {
        boolean available = llmService.isChatModelAvailable();

        String testResponse = null;
        if (available) {
            testResponse = llmService.chat(
                    "Say 'Hello, I am ready to help with movie recommendations!'",
                    "You are a movie assistant."
            );
        }

        return ResponseEntity.ok(Map.of(
                "llmAvailable", available,
                "model", "llama3.2",
                "testResponse", testResponse != null ? testResponse : "Model not available"
        ));
    }

    /**
     * Get chat system status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean chatModelAvailable = llmService.isChatModelAvailable();

        return ResponseEntity.ok(Map.of(
                "chatModelAvailable", chatModelAvailable,
                "chatModel", "llama3.2",
                "embeddingModel", "nomic-embed-text",
                "ragEnabled", true
        ));
    }
}