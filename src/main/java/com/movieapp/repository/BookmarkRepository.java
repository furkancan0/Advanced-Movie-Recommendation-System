package com.movieapp.repository;

import com.movieapp.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserIdAndMovieId(Long userId, Long movieId);

    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    Page<Bookmark> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all bookmarks for a user (no pagination)
     * Used for recommendation system to analyze all bookmarked movies
     */
    List<Bookmark> findAllByUserId(Long userId);

    /**
     * Get all movie IDs bookmarked by user
     */
    @Query("SELECT b.movie.id FROM Bookmark b WHERE b.user.id = :userId")
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    /**
     * Delete bookmark by user and movie ID
     */
    void deleteByUserIdAndMovieId(Long userId, Long movieId);

    /**
     * Count bookmarks for a user
     */
    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * Find bookmarks with movie details eagerly loaded
     */
    @Query("SELECT b FROM Bookmark b JOIN FETCH b.movie WHERE b.user.id = :userId")
    List<Bookmark> findAllByUserIdWithMovies(@Param("userId") Long userId);
}