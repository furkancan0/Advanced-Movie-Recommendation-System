package com.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDTO {
    private Long id;
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private String overview;
    private LocalDate releaseDate;
    private String posterPath;
    private String backdropPath;
    private Double voteAverage;
    private Integer voteCount;
    private Double popularity;
    private String originalLanguage;
    private List<GenreDTO> genres;
    private List<KeywordDTO> keywords;
    private List<String> cast;
    private List<String> directors;
    private Integer runtime;
    private Double avgRating;
    private Integer ratingCount;
    private Double bayesianRating;
}