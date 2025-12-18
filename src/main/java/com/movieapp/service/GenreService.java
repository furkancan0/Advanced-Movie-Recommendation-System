package com.movieapp.service;

import com.movieapp.dto.GenreDTO;
import com.movieapp.entity.Genre;
import com.movieapp.mapper.MovieMapper;
import com.movieapp.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional()
public class GenreService {
    private final GenreRepository genreRepository;

    @Cacheable(value = "all-genres")
    public List<GenreDTO> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "popular-genres")
    public List<GenreDTO> getPopularGenres() {
        return genreRepository.findPopularGenres().stream()
                .limit(20)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public GenreDTO getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Genre not found"));
        return mapToDTO(genre);
    }

    public Optional<Genre> findByTmdbId(Integer tmdbId) {
        return genreRepository.findByTmdbId(tmdbId);
    }

    public Optional<Genre> findByName(String name) {
        return genreRepository.findByName(name);
    }

    public List<Genre> findByTmdbIds(Set<Integer> tmdbIds) {
        return genreRepository.findByTmdbIdIn(tmdbIds);
    }

    @Transactional
    public Genre getOrCreateGenre(Integer tmdbId, String name) {
        return genreRepository.findByTmdbId(tmdbId)
                .orElseGet(() -> {
                    log.info("Creating new genre: {} (TMDb ID: {})", name, tmdbId);
                    Genre genre = Genre.builder()
                            .tmdbId(tmdbId)
                            .name(name)
                            .movieCount(0)
                            .build();
                    return genreRepository.save(genre);
                });
    }

    @Transactional
    public void incrementMovieCount(Long genreId) {
        genreRepository.findById(genreId).ifPresent(genre -> {
            genre.setMovieCount(genre.getMovieCount() + 1);
            genreRepository.save(genre);
        });
    }

    @Transactional
    public void decrementMovieCount(Long genreId) {
        genreRepository.findById(genreId).ifPresent(genre -> {
            genre.setMovieCount(Math.max(0, genre.getMovieCount() - 1));
            genreRepository.save(genre);
        });
    }

    private GenreDTO mapToDTO(Genre genre) {
        return GenreDTO.builder()
                .id(genre.getId())
                .tmdbId(genre.getTmdbId())
                .name(genre.getName())
                .description(genre.getDescription())
                .movieCount(genre.getMovieCount())
                .build();
    }
}
