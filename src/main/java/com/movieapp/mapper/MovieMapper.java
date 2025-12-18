package com.movieapp.mapper;

import com.movieapp.dto.GenreDTO;
import com.movieapp.dto.KeywordDTO;
import com.movieapp.dto.MovieDTO;
import com.movieapp.dto.TMDbMovieResponse;
import com.movieapp.entity.Genre;
import com.movieapp.entity.Movie;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MovieMapper {
    public MovieDTO mapToDTO(Movie movie) {
        List<GenreDTO> genreDTOs = movie.getGenres().stream()
                .map(genre -> GenreDTO.builder()
                        .id(genre.getId())
                        .tmdbId(genre.getTmdbId())
                        .name(genre.getName())
                        .build())
                .collect(Collectors.toList());

        List<KeywordDTO> keywordDTOs = movie.getKeywords().stream()
                .map(keyword -> KeywordDTO.builder()
                        .id(keyword.getId())
                        .tmdbId(keyword.getTmdbId())
                        .name(keyword.getName())
                        .build())
                .collect(Collectors.toList());

        return MovieDTO.builder()
                .id(movie.getId())
                .tmdbId(movie.getTmdbId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .overview(movie.getOverview())
                .releaseDate(movie.getReleaseDate())
                .posterPath(movie.getPosterPath())
                .backdropPath(movie.getBackdropPath())
                .voteAverage(movie.getVoteAverage())
                .voteCount(movie.getVoteCount())
                .popularity(movie.getPopularity())
                .originalLanguage(movie.getOriginalLanguage())
                .genres(genreDTOs)
                .keywords(keywordDTOs)
                .cast(movie.getCast() != null ? List.copyOf(movie.getCast()) : List.of())
                .directors(movie.getDirectors() != null ? List.copyOf(movie.getDirectors()) : List.of())
                .runtime(movie.getRuntime())
                .avgRating(movie.getAvgRating())
                .ratingCount(movie.getRatingCount())
                .build();
    }

    public MovieDTO mapToMovieDTO(TMDbMovieResponse response) {
        if (response == null) return null;

        /*List<GenreDTO> genres = response.getGenre_ids() != null
                ? response.getGenre_ids().stream()
                .map(g -> GenreDTO.builder()
                        .tmdbId(g)
                        .build())
                .collect(Collectors.toList())
                : List.of();*/

        List<GenreDTO> genres = List.of();
        if (response.getKeywords() != null && response.getGenres() != null) {
            genres = response.getGenres().stream()
                    .map(k -> GenreDTO.builder()
                            .tmdbId(k.getId())
                            .name(k.getName())
                            .build())
                    .collect(Collectors.toList());
        }

        List<KeywordDTO> keywords = List.of();
        if (response.getKeywords() != null && response.getKeywords().getKeywords() != null) {
            keywords = response.getKeywords().getKeywords().stream()
                    .map(k -> KeywordDTO.builder()
                            .tmdbId(k.getId())
                            .name(k.getName())
                            .build())
                    .collect(Collectors.toList());
        }

        List<String> cast = List.of();
        List<String> directors = List.of();

        if (response.getCredits() != null) {
            if (response.getCredits().getCast() != null) {
                cast = response.getCredits().getCast().stream()
                        .limit(10)
                        .map(TMDbMovieResponse.CastMember::getName)
                        .collect(Collectors.toList());
            }

            if (response.getCredits().getCrew() != null) {
                directors = response.getCredits().getCrew().stream()
                        .filter(c -> "Director".equals(c.getJob()))
                        .map(TMDbMovieResponse.CrewMember::getName)
                        .collect(Collectors.toList());
            }
        }

        return MovieDTO.builder()
                .tmdbId(response.getId())
                .title(response.getTitle())
                .originalTitle(response.getOriginalTitle())
                .overview(response.getOverview())
                .releaseDate(response.getReleaseDate())
                .posterPath(response.getPosterPath())
                .backdropPath(response.getBackdropPath())
                .voteAverage(response.getVoteAverage())
                .voteCount(response.getVoteCount())
                .popularity(response.getPopularity())
                .originalLanguage(response.getOriginalLanguage())
                .genres(genres)
                .keywords(keywords)
                .cast(cast)
                .directors(directors)
                .runtime(response.getRuntime())
                .build();
    }

    public List<MovieDTO> fromTMDbResponseList(List<TMDbMovieResponse> responses) {
        if (responses == null) return new ArrayList<>();
        return responses.stream()
                .map(this::mapToMovieDTO)
                .collect(Collectors.toList());
    }

}
