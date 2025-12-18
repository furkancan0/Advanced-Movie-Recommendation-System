package com.movieapp.service;

import com.movieapp.entity.Movie;
import com.movieapp.entity.Genre;
import com.movieapp.entity.Keyword;
import com.movieapp.repository.MovieRepository;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieEmbeddingService {

    private final OllamaEmbeddingService ollamaService;
    private final MovieRepository movieRepository;

    /**
     * Generate embedding for a movie based on overview, genres, and keywords
     */
    @Transactional(noRollbackFor = RuntimeException.class)
    public void generateMovieEmbedding(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (movie == null) {
            log.warn("Cannot generate embedding for null movie");
            return;
        }

        log.info("Generating embedding for movie: {} (ID: {})", movie.getTitle(), movie.getId());

        // Construct text representation of the movie
        String movieText = buildMovieText(movie);

        System.out.println("movieText: " + movieText);

        // Generate embedding using Ollama
        float[] embedding = ollamaService.generateEmbedding(movieText);
        PGvector pgVector = ollamaService.toPGVector(embedding);

        // Save to database
        movie.setEmbedding(pgVector);
        movie.setEmbeddingGeneratedAt(LocalDateTime.now());
        movieRepository.save(movie);

        log.info("Successfully generated and saved embedding for movie: {}", movie.getTitle());
    }

    /**
     * Build text representation of movie for embedding generation
     * Format: "title. overview. Genres: genre1, genre2. Keywords: keyword1, keyword2"
     */
    private String buildMovieText(Movie movie) {
        StringBuilder text = new StringBuilder();

        // Add title (important for matching)
        if (movie.getTitle() != null && !movie.getTitle().isEmpty()) {
            text.append(movie.getTitle()).append(". ");
        }

        // Add overview (main content)
        if (movie.getOverview() != null && !movie.getOverview().isEmpty()) {
            text.append(movie.getOverview()).append(". ");
        }

        // Add genres
        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            String genres = movie.getGenres().stream()
                    .map(Genre::getName)
                    .collect(Collectors.joining(", "));
            text.append("Genres: ").append(genres).append(". ");
        }

        // Add keywords
        if (movie.getKeywords() != null && !movie.getKeywords().isEmpty()) {
            String keywords = movie.getKeywords().stream()
                    .map(Keyword::getName)
                    .limit(10) // Limit to top 10 keywords
                    .collect(Collectors.joining(", "));
            text.append("Keywords: ").append(keywords).append(". ");
        }

        String result = text.toString().trim();
        log.debug("Built movie text ({} chars): {}", result.length(),
                result.substring(0, Math.min(100, result.length())) + "...");

        return result;
    }

    /**
     * Check if movie needs embedding regeneration
     */
    public boolean needsEmbeddingRegeneration(Movie movie) {
        if (movie.getEmbedding() == null) {
            return true;
        }

        // Regenerate if embedding is older than 90 days
        if (movie.getEmbeddingGeneratedAt() == null ||
                movie.getEmbeddingGeneratedAt().isBefore(LocalDateTime.now().minusDays(90))) {
            return true;
        }

        return false;
    }

}