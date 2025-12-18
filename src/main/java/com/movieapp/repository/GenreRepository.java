package com.movieapp.repository;

import com.movieapp.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByTmdbId(Integer tmdbId);

    Optional<Genre> findByName(String name);

    List<Genre> findByTmdbIdIn(Set<Integer> tmdbIds);

    List<Genre> findByNameIn(Set<String> names);

    @Query("SELECT g FROM Genre g ORDER BY g.movieCount DESC")
    List<Genre> findPopularGenres();

    @Query("SELECT g FROM Genre g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Genre> searchByName(@Param("query") String query);

    boolean existsByTmdbId(Integer tmdbId);
}