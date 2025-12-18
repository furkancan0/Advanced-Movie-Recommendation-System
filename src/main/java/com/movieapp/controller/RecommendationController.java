package com.movieapp.controller;

import com.movieapp.dto.MovieDTO;
import com.movieapp.dto.VectorSimilarityResult;
import com.movieapp.entity.User;
import com.movieapp.service.RecommendationService;
import com.movieapp.service.VectorSearchService;
import com.movieapp.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final VectorSearchService vectorSearchService;
    private final RateLimiter rateLimiter;

    @GetMapping
    public ResponseEntity<List<MovieDTO>> getRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "12") int limit) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        List<MovieDTO> recommendations = recommendationService
                .getRecommendationsForUser(user.getId(), limit);

        // Return empty list if user has no activity (ratings)
        if (recommendations.isEmpty()) {
            return ResponseEntity.ok(recommendations); // Returns []
        }

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get VECTOR-BASED recommendations (semantic similarity using embeddings)
     * This uses user's preference vector and movie embeddings for semantic matching
     */
    @GetMapping("/vector-based")
    public ResponseEntity<List<VectorSimilarityResult>> getVectorBasedRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "20") int limit) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        List<VectorSimilarityResult> recommendations = vectorSearchService
                .getVectorBasedRecommendations(user.getId(), limit);

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Check if user has enough data for recommendations
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkRecommendationStatus(
            @AuthenticationPrincipal User user) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        Map<String, Object> stats = recommendationService.getUserRatingStatistics(user.getId());

        return ResponseEntity.ok(stats);
    }
}
