package com.movieapp.service;

import com.movieapp.dto.KeywordDTO;
import com.movieapp.entity.Keyword;
import com.movieapp.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordService {

    private final KeywordRepository keywordRepository;

    @Cacheable(value = "popular-keywords")
    public List<KeywordDTO> getPopularKeywords(int limit) {
        return keywordRepository.findPopularKeywords().stream()
                .limit(limit)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public KeywordDTO getKeywordById(Long id) {
        Keyword keyword = keywordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Keyword not found"));
        return mapToDTO(keyword);
    }

    public Optional<Keyword> findByTmdbId(Integer tmdbId) {
        return keywordRepository.findByTmdbId(tmdbId);
    }

    public Optional<Keyword> findByName(String name) {
        return keywordRepository.findByName(name);
    }

    public List<Keyword> findByTmdbIds(Set<Integer> tmdbIds) {
        return keywordRepository.findByTmdbIdIn(tmdbIds);
    }

    @Transactional
    public Keyword getOrCreateKeyword(Integer tmdbId, String name) {
        return keywordRepository.findByTmdbId(tmdbId)
                .orElseGet(() -> {
                    log.debug("Creating new keyword: {} (TMDb ID: {})", name, tmdbId);
                    Keyword keyword = Keyword.builder()
                            .tmdbId(tmdbId)
                            .name(name)
                            .movieCount(0)
                            .build();
                    return keywordRepository.save(keyword);
                });
    }

    @Transactional
    public void incrementMovieCount(Long keywordId) {
        keywordRepository.findById(keywordId).ifPresent(keyword -> {
            keyword.setMovieCount(keyword.getMovieCount() + 1);
            keywordRepository.save(keyword);
        });
    }

    @Transactional
    public void decrementMovieCount(Long keywordId) {
        keywordRepository.findById(keywordId).ifPresent(keyword -> {
            keyword.setMovieCount(Math.max(0, keyword.getMovieCount() - 1));
            keywordRepository.save(keyword);
        });
    }

    private KeywordDTO mapToDTO(Keyword keyword) {
        return KeywordDTO.builder()
                .id(keyword.getId())
                .tmdbId(keyword.getTmdbId())
                .name(keyword.getName())
                .movieCount(keyword.getMovieCount())
                .build();
    }
}