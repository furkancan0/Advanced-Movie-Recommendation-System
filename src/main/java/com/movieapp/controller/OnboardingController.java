package com.movieapp.controller;

import com.movieapp.dto.MovieDTO;
import com.movieapp.dto.OnboardingMovieDTO;
import com.movieapp.entity.User;
import com.movieapp.service.OnboardingService;
import com.movieapp.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final RateLimiter rateLimiter;

    /**
     * Get onboarding status
     * Returns completion status and progress
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getOnboardingStatus(
            @AuthenticationPrincipal User user) {

        //rateLimiter.checkRateLimit("user:" + user.getId());

        Map<String, Object> status = onboardingService.getOnboardingProgress(user.getId());
        return ResponseEntity.ok(status);
    }

    /**
     * Get diverse popular movies for onboarding
     * Returns 10 unrated movies from different genres
     * User can rate 3-10 of these movies
     */
    @GetMapping("/movies")
    public ResponseEntity<List<MovieDTO>> getOnboardingMovies(
            @AuthenticationPrincipal User user) {

        //rateLimiter.checkRateLimit("user:" + user.getId());

        List<MovieDTO> movies = onboardingService.getOnboardingMovies(user.getId());

        return ResponseEntity.ok(movies);
    }

    /**
     * Get more onboarding movies
     * When user wants to rate more movies to improve recommendations
     */
    @GetMapping("/movies/more")
    public ResponseEntity<List<MovieDTO>> getMoreOnboardingMovies(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") int limit) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        List<MovieDTO> movies = onboardingService.getOnboardingMovies(user.getId());

        return ResponseEntity.ok(movies);
    }
}