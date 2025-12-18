package com.movieapp.repository;

import com.movieapp.entity.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT c.messageCount FROM ChatConversation c WHERE c.user.id = :userId")
    Integer getMessageCountByUserId(@Param("userId") Long userId);
}