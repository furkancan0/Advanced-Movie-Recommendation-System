package com.movieapp.service;

import com.movieapp.dto.VectorSimilarityResult;
import com.movieapp.entity.Movie;
import com.movieapp.entity.Rating;
import com.movieapp.entity.User;
import com.movieapp.repository.MovieRepository;
import com.movieapp.repository.RatingRepository;
import com.movieapp.repository.UserRepository;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VectorSearchService {

    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final OllamaEmbeddingService ollamaService;

    /**
     * Find similar movies using vector similarity (content-based)
     */
    @Cacheable(value = "similar-movies", key = "#movieId + '-' + #limit")
    public List<VectorSimilarityResult> findSimilarMovies(Long movieId, int limit) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (movie.getEmbedding() == null) {
            log.warn("Movie {} has no embedding, cannot find similar movies", movieId);
            return List.of();
        }

        log.info("Finding {} similar movies for: {}", limit, movie.getTitle());

        // Convert PGvector to string format for query
        String vectorStr = pgVectorToString(movie.getEmbedding());

        // Execute KNN search
        List<Object[]> results = movieRepository.findSimilarMoviesByVector(
                vectorStr,
                movieId,
                limit
        );

        return mapToVectorSimilarityResults(results);
    }

    /**
     * Get recommendations for user based on their preference vector
     */
    @Cacheable(value = "user-vector-recommendations", key = "#userId + '-' + #limit")
    public List<VectorSimilarityResult> getVectorBasedRecommendations(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPreferenceVector() == null) {
            log.warn("User {} has no preference vector, cannot generate vector-based recommendations", userId);
            return List.of();
        }

        log.info("Generating vector-based recommendations for user: {}", user.getUsername());

        // Get movies user has already rated
        List<Rating> userRatings = ratingRepository.findByUserId(userId);
        List<Long> excludeMovieIds = userRatings.stream()
                .map(r -> r.getMovie().getId())
                .collect(Collectors.toList());

        if (excludeMovieIds.isEmpty()) {
            excludeMovieIds.add(-1L); // Dummy value to avoid empty list in SQL
        }

        // Convert user vector to string
        String vectorStr = pgVectorToString(user.getPreferenceVector());

        // Execute KNN search
        List<Object[]> results = movieRepository.findMoviesByUserVector(
                vectorStr,
                excludeMovieIds,
                limit
        );

        return mapToVectorSimilarityResults(results);
    }

    /**
     * Convert PGvector to PostgreSQL vector string format
     * Example: [0.1, 0.2, 0.3, ...]
     */
    private String pgVectorToString(PGvector vector) {
        if (vector == null) {
            return "[0]";
        }

        float[] array = vector.toArray();
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * Map native query results to VectorSimilarityResult objects
     */
    private List<VectorSimilarityResult> mapToVectorSimilarityResults(List<Object[]> results) {
        List<VectorSimilarityResult> similarities = new ArrayList<>();

        for (Object[] row : results) {
            VectorSimilarityResult result = VectorSimilarityResult.builder()
                    .id(((Long) row[0]))
                    .tmdbId(((Long) row[1]))
                    .title((String) row[2])
                    .posterPath((String) row[3])
                    .avgRating(row[4] != null ? ((Double) row[4]) : null)
                    .releaseDate(((Date) row[5]).toLocalDate())
                    .similarity(((Number) row[6]).doubleValue())
                    .build();

            similarities.add(result);
        }

        log.debug("Mapped {} vector similarity results", similarities.size());
        return similarities;
    }

    /**
     * Find movies similar to a text query (search by semantic meaning)
     */
    @Cacheable(value = "similar-movies", key = "#query")
    public List<VectorSimilarityResult> searchMoviesBySemanticMeaning(String query, int limit) {
        log.info("Semantic search for: {}", query);

        // Generate embedding for the search query
        float[] queryEmbedding = ollamaService.generateEmbedding(query);
        String vectorStr = floatArrayToString(queryEmbedding);

        // Search using KNN
        List<Object[]> results = movieRepository.findSimilarMoviesByVector(
                vectorStr,
                -1L, // Don't exclude any movie
                limit
        );

        return mapToVectorSimilarityResults(results);
    }

    private String floatArrayToString(float[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}