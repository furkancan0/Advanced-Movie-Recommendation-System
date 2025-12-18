package com.movieapp.controller;

import com.movieapp.dto.MovieDTO;
import com.movieapp.dto.UserDTO;
import com.movieapp.entity.User;
import com.movieapp.service.OnboardingService;
import com.movieapp.util.RateLimiter;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final OnboardingService onboardingService;
    private final RateLimiter rateLimiter;

    /**
     * Get current user's profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(@AuthenticationPrincipal User user) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        UserDTO dto = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .onboardingCompleted(user.getOnboardingCompleted()) // Based on rating count
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(dto);
    }

    /**
     * Get famous movies for onboarding - PUBLIC
     * Returns 2-3 famous movies per genre for user to rate
     * User should rate 3-10 movies to complete onboarding
     */
    @GetMapping("/onboarding/movies")
    public ResponseEntity<List<MovieDTO>> getOnboardingMovies(
            @AuthenticationPrincipal User user) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        List<MovieDTO> onboardingMovies = onboardingService.getOnboardingMovies(user.getId());

        return ResponseEntity.ok(onboardingMovies);
    }

    /**
     * Get onboarding progress - REQUIRES AUTHENTICATION
     */
    @GetMapping("/onboarding/progress")
    public ResponseEntity<Map<String, Object>> getOnboardingProgress(
            @AuthenticationPrincipal User user) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        Map<String, Object> progress = onboardingService.getOnboardingProgress(user.getId());
        return ResponseEntity.ok(progress);
    }
}