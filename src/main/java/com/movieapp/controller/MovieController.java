package com.movieapp.controller;

import com.movieapp.dto.VectorSimilarityResult;
import com.movieapp.entity.Movie;
import com.movieapp.repository.MovieRepository;
import com.movieapp.service.MovieEmbeddingService;
import com.movieapp.service.VectorSearchService;
import com.movieapp.util.RateLimiter;
import com.movieapp.dto.MovieDTO;
import com.movieapp.entity.User;
import com.movieapp.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;
    private final MovieEmbeddingService embeddingService;
    private final MovieRepository movieRepository;
    private final VectorSearchService vectorSearchService;
    private final RateLimiter rateLimiter;

    @PostMapping("/{id}")
    public ResponseEntity<MovieDTO> syncToTmdb(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
            System.out.println("user:" + user.getUsername());
        } else {
            rateLimiter.checkRateLimit("anonymous");
        }

        MovieDTO movie = movieService.syncToTmdb(id);
        return ResponseEntity.ok(movie);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDTO> getMovie(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
            System.out.println("user:" + user.getUsername());
        } else {
            rateLimiter.checkRateLimit("anonymous");
        }

        MovieDTO movie = movieService.getMovie(id);
        return ResponseEntity.ok(movie);
    }

    @GetMapping("/tmdb/{tmdbId}")
    public ResponseEntity<MovieDTO> getMovieByTmdbId(
            @PathVariable Long tmdbId,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
        } else {
            rateLimiter.checkRateLimit("anonymous");
        }

        MovieDTO movie = movieService.getOrFetchMovie(tmdbId);
        return ResponseEntity.ok(movie);
    }


    @GetMapping("/search")
    public ResponseEntity<Page<MovieDTO>> searchMovies(
            @RequestParam String query,
            Pageable pageable,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
        } else {
            rateLimiter.checkRateLimit("anonymous");
        }

        Page<MovieDTO> results = movieService.searchMovies(query, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Semantic search - find movies by meaning, not just keywords
     */
    @GetMapping("/semantic-search")
    public ResponseEntity<List<VectorSimilarityResult>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {

        rateLimiter.checkRateLimit("anonymous");

        List<VectorSimilarityResult> results = vectorSearchService
                .searchMoviesBySemanticMeaning(query, limit);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<MovieDTO>> getPopularMovies(
            @RequestParam(defaultValue = "18") int limit,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
        } else {
            rateLimiter.checkRateLimit("anonymous");
        }
        List<MovieDTO> movies = movieService.getPopularMovies(limit);

        return ResponseEntity.ok(movies);
    }

    @GetMapping("/similar")
    public ResponseEntity<List<VectorSimilarityResult>> getSimilarMovies(
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
        } else {
            rateLimiter.checkRateLimit("anonymous");
        }

        List<VectorSimilarityResult> similarMovies = vectorSearchService.findSimilarMovies(34L, limit);
        return ResponseEntity.ok(similarMovies);
    }

    @GetMapping("/by-genres")
    public ResponseEntity<List<MovieDTO>> getMoviesByGenres(
            @RequestParam List<String> genres,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
        }

        List<MovieDTO> movies = movieService.getMoviesByGenres(genres, limit);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/genre/{genreName}")
    public ResponseEntity<String> getMoviesByGenre(
            @PathVariable String genreName,
            @RequestParam(defaultValue = "4") int limit,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
        } else {
            rateLimiter.checkRateLimit("anonymous");
        }

        String str = movieService.getMoviesByGenre(genreName, limit);
        return ResponseEntity.ok(str);
    }

    /**
     * Generate embedding for specific movie
     */
    @PostMapping("/generate/{movieId}")
    public ResponseEntity<Map<String, String>> generateMovieEmbedding(@PathVariable Long movieId) {
        embeddingService.generateMovieEmbedding(movieId);

        return ResponseEntity.ok(Map.of(
                "message", "Embedding generated successfully",
                "movieId", movieId.toString()
        ));
    }

    @PostMapping("/fetch-for-movie")
    public ResponseEntity<Map<String, String>> fetchKeywordsForMovie() {
        try {
            //movieService.fetchAndSaveKeywordsForMovie(movieId);
            return ResponseEntity.ok(Map.of(
                    "message", "Keywords fetched and saved successfully",
                    "movieId", String.valueOf(4)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "movieId", String.valueOf(4)
            ));
        }
    }
}
