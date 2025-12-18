package com.movieapp.repository;

import com.movieapp.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessagesByConversation(
            @Param("conversationId") Long conversationId
    );

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId")
    Long countByConversationId(@Param("conversationId") Long conversationId);
}