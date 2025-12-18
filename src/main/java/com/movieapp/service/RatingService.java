package com.movieapp.service;

import com.movieapp.dto.RatingDTO;
import com.movieapp.entity.Movie;
import com.movieapp.entity.Rating;
import com.movieapp.entity.User;
import com.movieapp.mapper.RatingMapper;
import com.movieapp.repository.MovieRepository;
import com.movieapp.repository.RatingRepository;
import com.movieapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final UserVectorService userVectorService;
    private final RatingMapper ratingMapper;

    @Transactional
    @CacheEvict(value = {"user-recommendations", "user-vector-recommendations", "movie-details"}, allEntries = true)
    public RatingDTO rateMovie(Long userId, Long movieId, Integer ratingValue, String review) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // Check if rating already exists
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndMovieId(userId, movieId);

        Rating rating;
        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            log.info("Updating rating for user {} on movie {}: {} -> {}",
                    userId, movieId, rating.getRating(), ratingValue);
            rating.setRating(ratingValue);
            rating.setReview(review);
        } else {
            // Create new rating
            log.info("Creating new rating for user {} on movie {}: {}",
                    userId, movieId, ratingValue);
            rating = Rating.builder()
                    .user(user)
                    .movie(movie)
                    .rating(ratingValue)
                    .review(review)
                    .build();
        }

        rating = ratingRepository.save(rating);

        // Update movie's average rating
        updateMovieRatingStats(movieId);

        try {
            userVectorService.updateUserVectorAfterRating(userId, movieId);
        } catch (Exception e) {
            log.error("Error updating user vector: {}", e.getMessage());
            // Don't fail the rating operation if vector update fails
        }


        return ratingMapper.toRatingDTO(rating);
    }

    @Transactional
    @CacheEvict(value = {"user-recommendations", "movie-details"}, allEntries = true)
    public void deleteRating(Long userId, Long movieId) {
        Rating rating = ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        log.info("Deleting rating for user {} on movie {}", userId, movieId);
        ratingRepository.delete(rating);

        // Update movie's average rating
        updateMovieRatingStats(movieId);

        try {
            userVectorService.updateUserVector(userId);
        } catch (Exception e) {
            log.error("Error updating user vector after deletion: {}", e.getMessage());
        }
    }

    @Transactional
    protected void updateMovieRatingStats(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        Double avgRating = ratingRepository.calculateAverageRating(movieId);
        List<Rating> ratings = ratingRepository.findByMovieId(movieId);

        movie.setAvgRating(avgRating);
        movie.setRatingCount(ratings.size());
        movieRepository.save(movie);

        log.debug("Updated rating stats for movie {}: avg={}, count={}",
                movieId, avgRating, ratings.size());
    }

    public Optional<RatingDTO> getUserRatingForMovie(Long userId, Long movieId) {
        return ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .map(ratingMapper::toRatingDTO);
    }

    public Page<RatingDTO> getUserRatings(Long userId, Pageable pageable) {
        return ratingRepository.findByUserId(userId, pageable)
                .map(ratingMapper::toRatingDTO);
    }

    public List<RatingDTO> getMovieRatings(Long movieId) {
        return ratingMapper.toRatingDTOList(ratingRepository.findByMovieId(movieId));
    }
}
