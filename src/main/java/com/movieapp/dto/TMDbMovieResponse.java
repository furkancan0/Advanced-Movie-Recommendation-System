package com.movieapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TMDbMovieResponse {
    private Long id;
    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    private String overview;

    @JsonProperty("release_date")
    private LocalDate releaseDate;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    private Double popularity;

    @JsonProperty("original_language")
    private String originalLanguage;

    private List<Genre> genres;
    private List<Integer> genre_ids;

    private Integer runtime;
    private Credits credits;
    private Keywords keywords;

    @Data
    public static class Genre {
        private Integer id;
        private String name;
    }

    @Data
    public static class Keywords {
        private List<Keyword> keywords;
    }

    @Data
    public static class Keyword {
        private Integer id;
        private String name;
    }

    @Data
    public static class Credits {
        private List<CastMember> cast;
        private List<CrewMember> crew;
    }

    @Data
    public static class CastMember {
        private Long id;
        private String name;
        private String character;
    }

    @Data
    public static class CrewMember {
        private Long id;
        private String name;
        private String job;
    }
}