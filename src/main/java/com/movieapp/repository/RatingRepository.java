package com.movieapp.repository;

import com.movieapp.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId);

    List<Rating> findByUserId(Long userId);

    Page<Rating> findByUserId(Long userId, Pageable pageable);

    List<Rating> findByMovieId(Long movieId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Rating r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Rating> findRecentRatingsByUser(@Param("userId") Long userId, Pageable pageable);

    // Find users who rated similar movies
    @Query("SELECT r.user.id FROM Rating r WHERE r.movie.id IN :movieIds AND r.user.id != :userId GROUP BY r.user.id HAVING COUNT(DISTINCT r.movie.id) >= :minCommon")
    List<Long> findSimilarUsers(@Param("userId") Long userId, @Param("movieIds") List<Long> movieIds, @Param("minCommon") Long minCommon);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.movie.id = :movieId")
    Double calculateAverageRating(@Param("movieId") Long movieId);
}
