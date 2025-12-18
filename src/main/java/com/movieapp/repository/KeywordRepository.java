package com.movieapp.repository;

import com.movieapp.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByTmdbId(Integer tmdbId);

    Optional<Keyword> findByName(String name);

    List<Keyword> findByTmdbIdIn(Set<Integer> tmdbIds);

    List<Keyword> findByNameIn(Set<String> names);

    @Query("SELECT k FROM Keyword k ORDER BY k.movieCount DESC")
    List<Keyword> findPopularKeywords();

    @Query("SELECT k FROM Keyword k WHERE LOWER(k.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Keyword> searchByName(@Param("query") String query);

    boolean existsByTmdbId(Integer tmdbId);
}