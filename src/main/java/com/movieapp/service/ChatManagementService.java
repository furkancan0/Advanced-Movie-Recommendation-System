package com.movieapp.service;

import com.movieapp.dto.ChatMessageDTO;
import com.movieapp.dto.ConversationHistoryDTO;
import com.movieapp.entity.ChatConversation;
import com.movieapp.entity.ChatMessage;
import com.movieapp.repository.ChatConversationRepository;
import com.movieapp.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatManagementService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    /**
     * Get user's conversation history (single mentor conversation)
     */
    public ConversationHistoryDTO getUserConversationHistory(Long userId) {
        ChatConversation conversation = conversationRepository.findByUserId(userId)
                .orElse(null);

        if (conversation == null) {
            return ConversationHistoryDTO.builder()
                    .conversationId(null)
                    .title("Movie Mentor")
                    .messages(List.of())
                    .build();
        }

        List<ChatMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        List<ChatMessageDTO> messageDTOs = messages.stream()
                .map(this::mapToMessageDTO)
                .collect(Collectors.toList());

        return ConversationHistoryDTO.builder()
                .conversationId(conversation.getId())
                .title(conversation.getTitle())
                .messages(messageDTOs)
                .build();
    }

    /**
     * Get recent conversation history (last N messages)
     */
    public ConversationHistoryDTO getRecentHistory(Long userId, int limit) {
        ChatConversation conversation = conversationRepository.findByUserId(userId)
                .orElse(null);

        if (conversation == null) {
            return ConversationHistoryDTO.builder()
                    .conversationId(null)
                    .title("Movie Mentor")
                    .messages(List.of())
                    .build();
        }

        List<ChatMessage> allMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // Get last N messages
        int start = Math.max(0, allMessages.size() - limit);
        List<ChatMessage> recentMessages = allMessages.subList(start, allMessages.size());

        List<ChatMessageDTO> messageDTOs = recentMessages.stream()
                .map(this::mapToMessageDTO)
                .collect(Collectors.toList());

        return ConversationHistoryDTO.builder()
                .conversationId(conversation.getId())
                .title(conversation.getTitle())
                .messages(messageDTOs)
                .build();
    }

    /**
     * Clear conversation history (start fresh with mentor)
     */
    @Transactional
    public void clearConversationHistory(Long userId) {
        ChatConversation conversation = conversationRepository.findByUserId(userId)
                .orElse(null);

        if (conversation != null) {
            // Delete all messages
            messageRepository.deleteAll(conversation.getMessages());
            conversation.getMessages().clear();
            conversation.setMessageCount(0);
            conversationRepository.save(conversation);

            log.info("Cleared conversation history for user: {}", userId);
        }
    }

    /**
     * Get conversation statistics
     */
    public ConversationStats getConversationStats(Long userId) {
        Integer messageCount = conversationRepository.getMessageCountByUserId(userId);

        return new ConversationStats(
                messageCount != null ? messageCount : 0,
                conversationRepository.existsByUserId(userId)
        );
    }

    /**
     * Map to ChatMessageDTO
     */
    private ChatMessageDTO mapToMessageDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .referencedMovieIds(message.getReferencedMovieIds())
                .isOffTopic(message.getIsOffTopic())
                .createdAt(message.getCreatedAt())
                .build();
    }

    // Stats class
    public record ConversationStats(Integer totalMessages, Boolean hasConversation) {}
}