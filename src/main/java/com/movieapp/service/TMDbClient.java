package com.movieapp.service;

import com.movieapp.dto.KeywordDTO;
import com.movieapp.dto.MovieDTO;
import com.movieapp.dto.TMDbMovieResponse;
import com.movieapp.dto.TMDbSearchResponse;
import com.movieapp.mapper.MovieMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TMDbClient {
    private final RestClient restClient;
    private final String apiKey;
    private final Bucket rateLimiter;

    private final MovieMapper movieMapper;

    public TMDbClient(
            @Value("${tmdb.api.key}") String apiKey,
            @Value("${tmdb.api.base-url}") String baseUrl,
            @Value("${tmdb.api.rate-limit.requests-per-second:4}") int requestsPerSecond, MovieMapper movieMapper){

        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.movieMapper = movieMapper;

        // TMDb allows 40 requests per 10 seconds, we'll be conservative
        Bandwidth limit = Bandwidth.classic(
                requestsPerSecond * 10L,
                Refill.intervally(requestsPerSecond * 10L, Duration.ofSeconds(10))
        );
        this.rateLimiter = Bucket.builder()
                .addLimit(limit)
                .build();

        log.info("TMDb Service initialized with rate limit: {} req/10s", requestsPerSecond * 10);
    }
    @Cacheable(value = "tmdb-movie", key = "#tmdbId", unless = "#result == null")
    public MovieDTO getMovieDetails(Long tmdbId) {
        checkRateLimit();
        log.debug("Fetching movie details from TMDb: {}", tmdbId);

        TMDbMovieResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("api_key", apiKey)
                        .queryParam("append_to_response", "credits,keywords")
                        .build(tmdbId))
                .retrieve()
                .body(TMDbMovieResponse.class);


        return movieMapper.mapToMovieDTO(response);
    }

    @Cacheable(value = "tmdb-search", key = "#query + '-' + #page", unless = "#result == null || #result.isEmpty()")
    public List<MovieDTO> searchMovies(String query, int page) {
        checkRateLimit();

        log.debug("Searching movies on TMDb: query={}, page={}", query, page);

        TMDbSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TMDbSearchResponse.class);

        if (response == null || response.getResults() == null) {
            return List.of();
        }

        return response.getResults().stream()
                .map(movieMapper::mapToMovieDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "tmdb-popular", key = "#page")
    public List<MovieDTO> getPopularMovies(int page) {
        checkRateLimit();

        log.debug("Fetching popular movies from TMDb: page={}", page);

        TMDbSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("api_key", apiKey)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TMDbSearchResponse.class);

        if (response == null || response.getResults() == null) {
            return List.of();
        }

        System.out.println("TMDbSearchResponse  "+ response.getResults());

        return response.getResults().stream()
                .map(movieMapper::mapToMovieDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "tmdb-genre", key = "#genreId + '-' + #page")
    public List<MovieDTO> getMoviesByGenre(Integer genreId, int page) {
        checkRateLimit();

        log.debug("Fetching movies by genre from TMDb: genreId={}, page={}", genreId, page);

        TMDbSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("with_genres", genreId)
                        .queryParam("append_to_response", "keywords")
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TMDbSearchResponse.class);

        if (response == null || response.getResults() == null) {
            return List.of();
        }

        return response.getResults().stream()
                .map(movieMapper::mapToMovieDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "tmdb-top-rated", key = "#page")
    public List<MovieDTO> getTopRatedMovies(int page) {
        checkRateLimit();

        log.debug("Fetching top rated movies from TMDb: page={}", page);

        TMDbSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/top_rated")
                        .queryParam("api_key", apiKey)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TMDbSearchResponse.class);

        if (response == null || response.getResults() == null) {
            return List.of();
        }

        return response.getResults().stream()
                .map(movieMapper::mapToMovieDTO)
                .collect(Collectors.toList());
    }

    @Data
    public static class TMDbKeywordsResponse {
        private Long id;
        private List<KeywordItem> keywords;

        @Data
        public static class KeywordItem {
            private Integer id;
            private String name;
        }
    }

    //@Cacheable(value = "tmdb-keywords", key = "#tmdbId", unless = "#result == null || #result.isEmpty()")
    public List<KeywordDTO> getMovieKeywords(Long tmdbId) {
        checkRateLimit();

        log.debug("Fetching keywords for movie {} from TMDb", tmdbId);

        try {
            TMDbKeywordsResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/movie/{id}/keywords")
                            .queryParam("api_key", apiKey)
                            .build(tmdbId))
                    .retrieve()
                    .body(TMDbKeywordsResponse.class);

            if (response == null || response.getKeywords() == null) {
                log.debug("No keywords found for movie {}", tmdbId);
                return List.of();
            }

            List<KeywordDTO> keywords = response.getKeywords().stream()
                    .map(k -> KeywordDTO.builder()
                            .tmdbId(k.getId())
                            .name(k.getName())
                            .build())
                    .collect(Collectors.toList());

            log.debug("Found {} keywords for movie {}", keywords.size(), tmdbId);
            return keywords;

        } catch (Exception e) {
            log.error("Error fetching keywords for movie {}: {}", tmdbId, e.getMessage());
            return List.of();
        }
    }

    private void checkRateLimit() {
        if (!rateLimiter.tryConsume(1)) {
            log.warn("TMDb API rate limit exceeded, waiting...");
            try {
                Thread.sleep(250); // Wait before retry
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limit wait interrupted", e);
            }
            if (!rateLimiter.tryConsume(1)) {
                throw new RuntimeException("TMDb API rate limit exceeded");
            }
        }
    }
}
