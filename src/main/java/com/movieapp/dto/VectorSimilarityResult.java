package com.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSimilarityResult {
    private Long id;
    private Long tmdbId;
    private String title;
    private Double similarity; // Cosine similarity score (0-1)
    private String posterPath;
    private Double avgRating;
    private LocalDate releaseDate;
}