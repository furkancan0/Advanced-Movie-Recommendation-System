package com.movieapp.repository;

import com.movieapp.entity.Genre;
import com.movieapp.entity.Keyword;
import com.movieapp.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByTmdbId(Long tmdbId);

    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Movie> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query(value = """
    SELECT id, title
    FROM movies
    WHERE title ILIKE '%' || :q || '%'
    ORDER BY similarity(title, :q) DESC
    LIMIT 10""", nativeQuery = true)
    Page<Movie> searchSuggest(@Param("query") String query, Pageable pageable);

    // Find movies by genres (ManyToMany relationship)
    @Query("SELECT DISTINCT m FROM Movie m JOIN m.genres g WHERE g IN :genres")
    List<Movie> findByGenresIn(@Param("genres") List<Genre> genres, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Movie m JOIN m.keywords k WHERE k IN :keywords")
    List<Movie> findByKeywordsIn(@Param("keywords") List<Keyword> keywords, Pageable pageable);

    // Find movies by single genre
    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g = :genre ORDER BY m.popularity DESC")
    List<Movie> findByGenre(@Param("genre") Genre genre, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Movie m JOIN m.genres g WHERE g.name IN :genreNames")
    List<Movie> findByGenreNames(@Param("genreNames") List<String> genreNames, Pageable pageable);

    // Find top rated movies
    @Query("SELECT m FROM Movie m WHERE m.avgRating IS NOT NULL ORDER BY m.avgRating DESC, m.ratingCount DESC")
    List<Movie> findTopRatedMovies(Pageable pageable);

    // Find popular movies
    @Query("SELECT m FROM Movie m WHERE m.popularity IS NOT NULL ORDER BY m.popularity DESC")
    List<Movie> findPopularMovies(Pageable pageable);

    // Find popular movies by genre for onboarding
    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g = :genre " +
            "AND m.popularity > 50 " +
            "ORDER BY m.popularity DESC, m.voteAverage DESC")
    List<Movie> findPopularMoviesByGenre(@Param("genre") Genre genre, Pageable pageable);

    // Find diverse popular movies across all genres for onboarding
    @Query("SELECT m FROM Movie m WHERE m.popularity > 100 " +
            "AND m.voteAverage > 7.0 " +
            "ORDER BY m.popularity DESC")
    List<Movie> findPopularMoviesForOnboarding(Pageable pageable);

    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g IN :genres ORDER BY m.avgRating DESC, m.popularity DESC")
    List<Movie> findTopMoviesByGenres(@Param("genres") List<Genre> genres, Pageable pageable);

    /**
     * Find similar movies using cosine distance (KNN search)
     * Returns movies with embedding ordered by similarity to the given vector
     */
    @Query(value = "SELECT m.id, m.tmdb_id, m.title, m.poster_path, m.avg_rating,m.release_date, " +
            "1 - (m.embedding <=> CAST(:vector AS vector)) as similarity " +
            "FROM movies m " +
            "WHERE m.embedding IS NOT NULL " +
            "AND m.id != :excludeMovieId " +
            "ORDER BY m.embedding <=> CAST(:vector AS vector) " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findSimilarMoviesByVector(
            @Param("vector") String vector,
            @Param("excludeMovieId") Long excludeMovieId,
            @Param("limit") int limit
    );

    /**
     * Find movies similar to user's preference vector
     */
    @Query(value = "SELECT m.id, m.tmdb_id, m.title, m.poster_path, m.avg_rating, m.release_date, " +
            "1 - (m.embedding <=> CAST(:userVector AS vector)) as similarity " +
            "FROM movies m " +
            "WHERE m.embedding IS NOT NULL " +
            "AND m.id NOT IN :excludeMovieIds " +
            "ORDER BY m.embedding <=> CAST(:userVector AS vector) " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findMoviesByUserVector(
            @Param("userVector") String userVector,
            @Param("excludeMovieIds") List<Long> excludeMovieIds,
            @Param("limit") int limit
    );

}