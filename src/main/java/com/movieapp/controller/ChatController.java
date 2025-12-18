package com.movieapp.controller;

import com.movieapp.dto.ChatMessageDTO;
import com.movieapp.dto.ChatRequest;
import com.movieapp.dto.ConversationHistoryDTO;
import com.movieapp.entity.User;
import com.movieapp.service.ChatManagementService;
import com.movieapp.service.MovieChatService;
import com.movieapp.util.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MovieChatService chatService;
    private final ChatManagementService managementService;
    private final RateLimiter rateLimiter;

    /**
     * Send a chat message to your Movie Mentor
     */
    @PostMapping("/send")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatRequest request) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        ChatMessageDTO response = chatService.chat(
                user.getId(),
                request.getMessage()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get full conversation history with your mentor
     */
    @GetMapping("/history")
    public ResponseEntity<ConversationHistoryDTO> getHistory(
            @AuthenticationPrincipal User user) {

        ConversationHistoryDTO history = managementService
                .getUserConversationHistory(user.getId());

        return ResponseEntity.ok(history);
    }

    /**
     * Get recent conversation history (last N messages)
     */
    @GetMapping("/history/recent")
    public ResponseEntity<ConversationHistoryDTO> getRecentHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") int limit) {

        ConversationHistoryDTO history = managementService
                .getRecentHistory(user.getId(), limit);

        return ResponseEntity.ok(history);
    }

    /**
     * Clear conversation history (start fresh)
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory(
            @AuthenticationPrincipal User user) {

        managementService.clearConversationHistory(user.getId());

        return ResponseEntity.ok(Map.of(
                "message", "Conversation history cleared. Starting fresh with your Movie Mentor!"
        ));
    }

    /**
     * Get conversation statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ChatManagementService.ConversationStats> getStats(
            @AuthenticationPrincipal User user) {

        var stats = managementService.getConversationStats(user.getId());
        return ResponseEntity.ok(stats);
    }
}