package com.movieapp.dto;

import com.movieapp.entity.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private MessageRole role;
    private String content;
    private List<Long> referencedMovieIds;
    private Boolean isOffTopic;
    private LocalDateTime createdAt;
}