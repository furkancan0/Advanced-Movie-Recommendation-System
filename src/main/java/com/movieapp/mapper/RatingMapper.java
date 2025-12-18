package com.movieapp.mapper;

import com.movieapp.dto.RatingDTO;
import com.movieapp.entity.Rating;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RatingMapper {
    public RatingDTO toRatingDTO(Rating rating) {
        if (rating == null) return null;

        return RatingDTO.builder()
                .id(rating.getId())
                .userId(rating.getUser() != null ? rating.getUser().getId() : null)
                .movieId(rating.getMovie() != null ? rating.getMovie().getId() : null)
                .movieTitle(rating.getMovie() != null ? rating.getMovie().getTitle() : null)
                .rating(rating.getRating())
                .review(rating.getReview())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }

    public List<RatingDTO> toRatingDTOList(List<Rating> ratings) {
        if (ratings == null) return new ArrayList<>();
        return ratings.stream()
                .map(this::toRatingDTO)
                .collect(Collectors.toList());
    }
}
