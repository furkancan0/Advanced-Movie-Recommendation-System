package com.movieapp.controller;

import com.movieapp.dto.GenreDTO;
import com.movieapp.dto.MovieDTO;
import com.movieapp.service.GenreService;
import com.movieapp.service.MovieService;
import com.movieapp.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;
    private final MovieService movieService;
    private final RateLimiter rateLimiter;

    /**
     * Get all genres (public endpoint)
     */
    @GetMapping
    public ResponseEntity<List<GenreDTO>> getAllGenres() {
        List<GenreDTO> genres = genreService.getAllGenres();
        return ResponseEntity.ok(genres);
    }

    /**
     * Get popular genres sorted by movie count
     */
    @GetMapping("/popular")
    public ResponseEntity<List<GenreDTO>> getPopularGenres() {
        List<GenreDTO> genres = genreService.getPopularGenres();
        return ResponseEntity.ok(genres);
    }

    /**
     * Get genre by ID
     */
    @GetMapping("/{genreId}")
    public ResponseEntity<GenreDTO> getGenreById(@PathVariable Long genreId) {
        GenreDTO genre = genreService.getGenreById(genreId);
        return ResponseEntity.ok(genre);
    }

    /**
     * Get movies by genre names
     */
    @GetMapping("/movies")
    public ResponseEntity<List<MovieDTO>> getMoviesByGenres(
            @RequestParam List<String> genres,
            @RequestParam(defaultValue = "20") int limit) {

        rateLimiter.checkRateLimit("anonymous");

        List<MovieDTO> movies = movieService.getMoviesByGenres(genres, limit);
        return ResponseEntity.ok(movies);
    }
}