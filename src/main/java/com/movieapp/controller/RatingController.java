package com.movieapp.controller;

import com.movieapp.dto.RatingDTO;
import com.movieapp.dto.RatingRequest;
import com.movieapp.entity.User;
import com.movieapp.service.RatingService;
import com.movieapp.util.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final RateLimiter rateLimiter;

    /**
     * Rate a movie - REQUIRES AUTHENTICATION
     * Only logged-in users can rate movies
     */
    @PostMapping
    public ResponseEntity<RatingDTO> rateMovie(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RatingRequest request) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        RatingDTO rating = ratingService.rateMovie(
                user.getId(),
                request.getMovieId(),
                request.getRating(),
                request.getReview()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }

    /**
     * Get current user's ratings - REQUIRES AUTHENTICATION
     */
    @GetMapping("/my-ratings")
    public ResponseEntity<Page<RatingDTO>> getMyRatings(
            @AuthenticationPrincipal User user,
            Pageable pageable) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        Page<RatingDTO> ratings = ratingService.getUserRatings(user.getId(), pageable);
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get all ratings for a specific movie - PUBLIC
     * Anyone can see ratings, but only authenticated users can add them
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<RatingDTO>> getMovieRatings(
            @PathVariable Long movieId,
            @AuthenticationPrincipal User user) {

        if (user != null) {
            rateLimiter.checkRateLimit("user:" + user.getId());
        } else {
            rateLimiter.checkRateLimit("anonymous");
        }

        List<RatingDTO> ratings = ratingService.getMovieRatings(movieId);
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get current user's rating for a specific movie - REQUIRES AUTHENTICATION
     */
    @GetMapping("/movie/{movieId}/my-rating")
    public ResponseEntity<RatingDTO> getMyRatingForMovie(
            @AuthenticationPrincipal User user,
            @PathVariable Long movieId) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        return ratingService.getUserRatingForMovie(user.getId(), movieId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a rating - REQUIRES AUTHENTICATION
     * Users can only delete their own ratings
     */
    @DeleteMapping("/movie/{movieId}")
    public ResponseEntity<Void> deleteRating(
            @AuthenticationPrincipal User user,
            @PathVariable Long movieId) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        ratingService.deleteRating(user.getId(), movieId);
        return ResponseEntity.noContent().build();
    }
}