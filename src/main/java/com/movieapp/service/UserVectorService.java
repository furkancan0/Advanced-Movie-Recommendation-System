package com.movieapp.service;

import com.movieapp.entity.Movie;
import com.movieapp.entity.Rating;
import com.movieapp.entity.User;
import com.movieapp.repository.MovieRepository;
import com.movieapp.repository.RatingRepository;
import com.movieapp.repository.UserRepository;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserVectorService {

    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final OllamaEmbeddingService ollamaService;

    /**
     * Update user's preference vector based on their ratings
     * Weighted average of rated movie embeddings
     */
    @Transactional
    public void updateUserVector(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Updating preference vector for user: {} (ID: {})", user.getUsername(), userId);

        // Get all user's ratings
        List<Rating> ratings = ratingRepository.findByUserId(userId);

        if (ratings.isEmpty()) {
            log.info("User {} has no ratings, skipping vector update", userId);
            return;
        }

        // Calculate weighted average of movie embeddings
        float[] userVector = calculateWeightedAverageVector(ratings);

        if (userVector == null) {
            log.warn("Failed to calculate user vector for user {}", userId);
            return;
        }

        // Update user's preference vector
        user.setPreferenceVector(ollamaService.toPGVector(userVector));
        user.setPreferenceVectorUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Successfully updated preference vector for user: {}", user.getUsername());
    }

    /**
     * Calculate weighted average of movie embeddings based on ratings
     * Rating weight: 5-star = 2.0, 4-star = 1.5, 3-star = 1.0, 1-2 star = 0.5
     */
    private float[] calculateWeightedAverageVector(List<Rating> ratings) {
        int embeddingDim = 768; // nomic-embed-text dimension
        float[] sumVector = new float[embeddingDim];
        double totalWeight = 0.0;
        int validMovies = 0;

        for (Rating rating : ratings) {
            Movie movie = rating.getMovie();

            // Skip if movie doesn't have embedding yet
            if (movie.getEmbedding() == null) {
                log.debug("Movie {} has no embedding, skipping", movie.getId());
                continue;
            }

            // Get rating weight
            double weight = getRatingWeight(rating.getRating());

            // Get movie embedding
            float[] movieEmbedding = ollamaService.toFloatArray(movie.getEmbedding());

            // Add weighted embedding to sum
            for (int i = 0; i < embeddingDim; i++) {
                sumVector[i] += movieEmbedding[i] * weight;
            }

            totalWeight += weight;
            validMovies++;
        }

        if (validMovies == 0) {
            log.warn("No valid movie embeddings found for weighted average");
            return null;
        }

        // Calculate average
        for (int i = 0; i < embeddingDim; i++) {
            sumVector[i] /= totalWeight;
        }

        log.debug("Calculated user vector from {} movies with total weight {}",
                validMovies, totalWeight);

        return sumVector;
    }

    /**
     * Get weight for a rating value
     */
    private double getRatingWeight(int rating) {
        return switch (rating) {
            case 5 -> 2.0;
            case 4 -> 1.5;
            case 3 -> 1.0;
            case 2 -> 0.5;
            case 1 -> 0.5;
            default -> 1.0;
        };
    }

    /**
     * Check if user vector needs update
     */
    public boolean needsVectorUpdate(User user) {
        if (user.getPreferenceVector() == null) {
            return true;
        }

        // Update if last update was more than 1 day ago
        if (user.getPreferenceVectorUpdatedAt() == null ||
                user.getPreferenceVectorUpdatedAt().isBefore(LocalDateTime.now().minusDays(1))) {
            return true;
        }

        return false;
    }

    /**
     * Update user vector after new rating
     * Called automatically when user rates a movie
     */
    @Transactional
    public void updateUserVectorAfterRating(Long userId, Long movieId) {
        log.debug("Updating user vector after rating: user={}, movie={}", userId, movieId);

        // Ensure movie has embedding
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (movie.getEmbedding() == null) {
            log.info("Movie {} has no embedding, generating now", movieId);
            // This will be handled by MovieEmbeddingService
        }

        // Update user vector
        updateUserVector(userId);
    }
}