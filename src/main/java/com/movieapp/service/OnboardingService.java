package com.movieapp.service;

import com.movieapp.dto.MovieDTO;
import com.movieapp.entity.Genre;
import com.movieapp.entity.Movie;
import com.movieapp.entity.User;
import com.movieapp.mapper.MovieMapper;
import com.movieapp.repository.MovieRepository;
import com.movieapp.repository.RatingRepository;
import com.movieapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional()
public class OnboardingService {
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final GenreService genreService;
    private final MovieMapper movieMapper;
    private final TMDbClient tmDbClient;

    // Genre -> TMDb ID mapping for famous movies
    private static final Map<String, List<Long>> FAMOUS_MOVIES_BY_GENRE = Map.ofEntries(
            Map.entry("Action", Arrays.asList(155L, 24428L, 198L, 680L, 47933L)),
            Map.entry("Drama", Arrays.asList(278L, 240L, 389L, 19404L, 13L)),
            Map.entry("Comedy", Arrays.asList(637L, 105L, 115L, 96L, 274L)),
            Map.entry("Sci-Fi", Arrays.asList(424L, 603L, 329L, 280L, 27205L)),
            Map.entry("Horror", Arrays.asList(694L, 539L, 346L, 4232L, 745L)),
            Map.entry("Romance", Arrays.asList(597L, 8681L, 10681L, 207L, 238L)),
            Map.entry("Thriller", Arrays.asList(680L, 807L, 769L, 745L, 77L)),
            Map.entry("Animation", Arrays.asList(12L, 585L, 129L, 10681L, 49026L))
    );

    private static final int MIN_RATINGS_FOR_ONBOARDING = 10;
    private static final int MAX_ONBOARDING_MOVIES = 40;
    private final MovieService movieService;

    /**
     * Check if user needs onboarding
     * User completes onboarding by rating 10 famous movies
     */
    public boolean needsOnboarding(Long userId) {
        int minRatings = ratedCount(userId);

        // User needs onboarding if they have fewer than minimum ratings
        return minRatings < MIN_RATINGS_FOR_ONBOARDING;
    }

    public int ratedCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<Long> ratedTmdbIds = ratingRepository.findByUserId(userId).stream()
                .map(r -> r.getMovie().getTmdbId())
                .collect(Collectors.toSet());

        int ratedCount = 0;

        for (List<Long> tmdbIds : FAMOUS_MOVIES_BY_GENRE.values()) {
            ratedCount += (int) tmdbIds.stream()
                    .filter(ratedTmdbIds::contains)
                    .count();
        }

        if(ratedCount >= MIN_RATINGS_FOR_ONBOARDING ){
            user.setOnboardingCompleted(true);
            userRepository.save(user);
        }

        return ratedCount;
    }

    /**
     * Get famous movies from different genres for onboarding
     * Returns 2-3 famous movies per genre (16-24 total)
     */
    public List<MovieDTO> getOnboardingMovies(Long userId) {
        log.info("Getting onboarding movies for user {}", userId);

        // Get movies user has already rated (to exclude them)
        Set<Long> ratedTmdbIds = ratingRepository.findByUserId(userId).stream()
                .map(r -> r.getMovie().getTmdbId())
                .collect(Collectors.toSet());

        List<MovieDTO> onboardingMovies = new ArrayList<>();

        // Get 2-3 famous movies from each genre
        for (Map.Entry<String, List<Long>> entry : FAMOUS_MOVIES_BY_GENRE.entrySet()) {
            String genre = entry.getKey();
            List<Long> tmdbIds = entry.getValue();

            // Filter out already rated movies
            List<Long> unratedIds = tmdbIds.stream()
                    .filter(id -> !ratedTmdbIds.contains(id))
                    .limit(3) // Take 3 per genre
                    .collect(Collectors.toList());

            // Fetch movies from TMDb
            for (Long tmdbId : unratedIds) {
                try {
                    MovieDTO movie = movieService.getOrFetchMovie(tmdbId);
                    if (movie != null) {
                        onboardingMovies.add(movie);
                        log.debug("Added onboarding movie: {} ({})", movie.getTitle(), genre);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch onboarding movie {}: {}", tmdbId, e.getMessage());
                }
            }
        }

        // Shuffle to mix genres
        Collections.shuffle(onboardingMovies);

        log.info("Returning {} onboarding movies for user {}", onboardingMovies.size(), userId);
        return onboardingMovies;
    }

    /**
     * Get onboarding progress for user
     */
    public Map<String, Object> getOnboardingProgress(Long userId) {
        int rateCount = ratedCount(userId);
        boolean completed = rateCount >= MIN_RATINGS_FOR_ONBOARDING;
        boolean neededOnboarding = needsOnboarding(userId);

        return Map.of(
                "minRequired", MIN_RATINGS_FOR_ONBOARDING,
                "maxRecommended", MAX_ONBOARDING_MOVIES,
                "completed", completed,
                "rateCount", rateCount,
                "needsOnboarding", neededOnboarding
        );
    }

}
