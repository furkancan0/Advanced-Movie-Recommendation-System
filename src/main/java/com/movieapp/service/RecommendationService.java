package com.movieapp.service;

import com.movieapp.dto.MovieDTO;
import com.movieapp.entity.*;
import com.movieapp.mapper.MovieMapper;
import com.movieapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final BookmarkRepository bookmarkRepository;

    private static final int MIN_RATINGS_FOR_COLLABORATIVE = 10;
    private static final int MIN_COMMON_RATINGS = 5;
    private static final double CONTENT_WEIGHT = 0.4;
    private static final double COLLABORATIVE_WEIGHT = 0.6;

    @Cacheable(value = "user-recommendations", key = "#userId + '-' + #limit")
    public List<MovieDTO> getRecommendationsForUser(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long ratingCount = ratingRepository.countByUserId(userId);

        log.info("Generating recommendations for user {} with {} ratings", userId, ratingCount);

        // If user has no ratings, return popular movies
        if (ratingCount == 0) {
            return getPopularMovies(userId, limit);
        }

        // Use hybrid approach if user has enough ratings
        if (ratingCount >= MIN_RATINGS_FOR_COLLABORATIVE) {
            return getHybridRecommendations(userId, limit);
        } else {
            // Use content-based for users with few ratings
            return getContentBasedRecommendations(userId, limit);
        }
    }

    /**
     * Get preferred genres from user's rated movies
     * Higher rated movies (4-5 stars) have more weight
     */
    private List<Genre> getPreferredGenresFromRatings(Long userId) {
        List<Rating> userRatings = ratingRepository.findByUserId(userId);

        // Map: Genre -> Total weighted score
        Map<Long, Double> genreScores = new HashMap<>();
        Map<Long, Genre> genreMap = new HashMap<>();

        for (Rating rating : userRatings) {
            Movie movie = rating.getMovie();
            int ratingValue = rating.getRating();

            // Weight: 5-star = 2.0, 4-star = 1.5, 3-star = 1.0, 1-2 star = 0.5
            double weight = switch (ratingValue) {
                case 5 -> 2.0;
                case 4 -> 1.5;
                case 3 -> 1.0;
                default -> 0.5;
            };

            // Add weight to each genre of this movie
            for (Genre genre : movie.getGenres()) {
                genreScores.merge(genre.getId(), weight, Double::sum);
                genreMap.putIfAbsent(genre.getId(), genre);
            }
        }

        // Sort genres by score and return top genres
        return genreScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(5) // Top 5 preferred genres
                .map(entry -> genreMap.get(entry.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Content-based filtering: recommend movies similar to what user liked
     */
    public List<MovieDTO> getContentBasedRecommendations(Long userId, int limit) {
        log.debug("Using content-based filtering for user {}", userId);

        // Get preferred genres from user's ratings
        List<Genre> preferredGenres = getPreferredGenresFromRatings(userId);

        if (preferredGenres.isEmpty()) {
            log.warn("No genre preferences found for user {}, returning popular movies", userId);
            return getPopularMovies(userId, limit);
        }

        log.debug("User {} preferred genres: {}", userId,
                preferredGenres.stream().map(Genre::getName).collect(Collectors.toList()));

        // Get movies user already rated/bookmarked
        List<Rating> userRatings = ratingRepository.findByUserId(userId);
        Set<Long> ratedMovieIds = userRatings.stream()
                .map(r -> r.getMovie().getId())
                .collect(Collectors.toSet());

        Set<Long> bookmarkedIds = new HashSet<>(bookmarkRepository.findMovieIdsByUserId(userId));

        // Find movies with similar characteristics
        List<Movie> candidateMovies = movieRepository.findTopMoviesByGenres(
                preferredGenres,
                PageRequest.of(0, limit * 3)
        );

        // Filter out already rated/bookmarked movies
        List<Movie> recommendations = candidateMovies.stream()
                .filter(m -> !ratedMovieIds.contains(m.getId()) && !bookmarkedIds.contains(m.getId()))
                .limit(limit)
                .toList();

        return recommendations.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Collaborative filtering: recommend based on similar users' preferences
     */
    public List<MovieDTO> getCollaborativeRecommendations(Long userId, int limit) {
        log.debug("Using collaborative filtering for user {}", userId);

        List<Rating> userRatings = ratingRepository.findByUserId(userId);
        if (userRatings.isEmpty()) {
            return List.of();
        }

        Set<Long> ratedMovieIds = userRatings.stream()
                .map(r -> r.getMovie().getId())
                .collect(Collectors.toSet());

        // Find similar users based on common ratings
        List<Long> movieIdsList = new ArrayList<>(ratedMovieIds);
        List<Long> similarUserIds = ratingRepository.findSimilarUsers(
                userId,
                movieIdsList,
                (long) MIN_COMMON_RATINGS
        );

        log.debug("Found {} similar users for user {}", similarUserIds.size(), userId);

        if (similarUserIds.isEmpty()) {
            return List.of();
        }

        // Calculate user similarity scores
        Map<Long, Double> userSimilarities = calculateUserSimilarities(userId, similarUserIds, userRatings);

        // Get top similar users
        List<Long> topSimilarUsers = userSimilarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();

        // Get movies highly rated by similar users
        Map<Long, Double> movieScores = new HashMap<>();

        for (Long similarUserId : topSimilarUsers) {
            Double similarity = userSimilarities.get(similarUserId);
            List<Rating> similarUserRatings = ratingRepository.findByUserId(similarUserId);

            for (Rating rating : similarUserRatings) {
                Long movieId = rating.getMovie().getId();
                if (!ratedMovieIds.contains(movieId) && rating.getRating() >= 4) {
                    movieScores.merge(movieId, rating.getRating() * similarity, Double::sum);
                }
            }
        }

        // Sort by score and get top movies
        List<Long> recommendedMovieIds = movieScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Movie> movies = movieRepository.findAllById(recommendedMovieIds);
        return movies.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Hybrid approach combining content-based and collaborative filtering
     */
    public List<MovieDTO> getHybridRecommendations(Long userId, int limit) {
        log.debug("Using hybrid filtering for user {}", userId);

        int contentLimit = (int) (limit * CONTENT_WEIGHT * 2);
        int collaborativeLimit = (int) (limit * COLLABORATIVE_WEIGHT * 2);

        List<MovieDTO> contentBased = getContentBasedRecommendations(userId, contentLimit);
        List<MovieDTO> collaborative = getCollaborativeRecommendations(userId, collaborativeLimit);

        // Merge and deduplicate
        Map<Long, MovieDTO> mergedMap = new LinkedHashMap<>();

        // Add collaborative first (higher weight)
        for (MovieDTO movie : collaborative) {
            mergedMap.put(movie.getTmdbId(), movie);
        }

        // Add content-based
        for (MovieDTO movie : contentBased) {
            if (!mergedMap.containsKey(movie.getTmdbId())) {
                mergedMap.put(movie.getTmdbId(), movie);
            }
        }

        return mergedMap.values().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get popular movies for users with no ratings
     */
    private List<MovieDTO> getPopularMovies(Long userId, int limit) {
        log.debug("Getting popular movies for user {} with no ratings", userId);

        Set<Long> bookmarkedIds = new HashSet<>(bookmarkRepository.findMovieIdsByUserId(userId));

        List<Movie> popularMovies = movieRepository.findPopularMovies(PageRequest.of(0, limit * 2));

        return popularMovies.stream()
                .filter(m -> !bookmarkedIds.contains(m.getId()))
                .limit(limit)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Calculate similarity between two users based on their ratings
     * Using Pearson correlation coefficient
     */
    private Map<Long, Double> calculateUserSimilarities(Long userId, List<Long> similarUserIds, List<Rating> userRatings) {
        Map<Long, Integer> userRatingsMap = userRatings.stream()
                .collect(Collectors.toMap(
                        r -> r.getMovie().getId(),
                        Rating::getRating
                ));

        Map<Long, Double> similarities = new HashMap<>();

        for (Long similarUserId : similarUserIds) {
            List<Rating> similarUserRatings = ratingRepository.findByUserId(similarUserId);
            Map<Long, Integer> similarUserRatingsMap = similarUserRatings.stream()
                    .collect(Collectors.toMap(
                            r -> r.getMovie().getId(),
                            Rating::getRating
                    ));

            // Find common movies
            Set<Long> commonMovies = new HashSet<>(userRatingsMap.keySet());
            commonMovies.retainAll(similarUserRatingsMap.keySet());

            if (commonMovies.size() >= MIN_COMMON_RATINGS) {
                double similarity = calculatePearsonCorrelation(
                        userRatingsMap,
                        similarUserRatingsMap,
                        commonMovies
                );
                similarities.put(similarUserId, similarity);
            }
        }

        return similarities;
    }

    private double calculatePearsonCorrelation(
            Map<Long, Integer> user1Ratings,
            Map<Long, Integer> user2Ratings,
            Set<Long> commonMovies) {

        if (commonMovies.isEmpty()) return 0.0;

        double sum1 = 0, sum2 = 0, sum1Sq = 0, sum2Sq = 0, productSum = 0;
        int n = commonMovies.size();

        for (Long movieId : commonMovies) {
            int rating1 = user1Ratings.get(movieId);
            int rating2 = user2Ratings.get(movieId);

            sum1 += rating1;
            sum2 += rating2;
            sum1Sq += rating1 * rating1;
            sum2Sq += rating2 * rating2;
            productSum += rating1 * rating2;
        }

        double numerator = productSum - (sum1 * sum2 / n);
        double denominator = Math.sqrt(
                (sum1Sq - sum1 * sum1 / n) * (sum2Sq - sum2 * sum2 / n)
        );

        if (denominator == 0) return 0.0;

        return numerator / denominator;
    }

    private MovieDTO mapToDTO(Movie movie) {
        return MovieDTO.builder()
                .id(movie.getId())
                .tmdbId(movie.getTmdbId())
                .title(movie.getTitle())
                .overview(movie.getOverview())
                .posterPath(movie.getPosterPath())
                .releaseDate(movie.getReleaseDate())
                .avgRating(movie.getAvgRating())
                .ratingCount(movie.getRatingCount())
                .build();
    }

    public Map<String, Object> getUserRatingStatistics(Long userId){
        Map<String, Object> stats = new HashMap<>();
        int countSize = ratingRepository.findByUserId(userId).size();
        boolean hasEnoughData = countSize > 0;

        stats.put("hasRecommendations", hasEnoughData);
        stats.put("totalRatings", countSize);

        return stats;
    }
}