package com.movieapp.service;

import com.movieapp.dto.GenreDTO;
import com.movieapp.dto.KeywordDTO;
import com.movieapp.dto.MovieDTO;
import com.movieapp.entity.Genre;
import com.movieapp.entity.Keyword;
import com.movieapp.entity.Movie;
import com.movieapp.mapper.MovieMapper;
import com.movieapp.repository.GenreRepository;
import com.movieapp.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieService {
    private final TMDbClient tmDbClient;
    private final MovieRepository movieRepository;
    private final KeywordService keywordService;
    private final GenreService genreService;
    private final MovieEmbeddingService embeddingService;
    private final MovieMapper movieMapper;

    // Bayesian Average parameters
    @Value("${recommendation.bayesian.min-votes:25}")
    private int minimumVotes; // C: Minimum votes required (confidence threshold)

    @Value("${recommendation.bayesian.global-mean:3.5}")
    private double globalMeanRating; // m: Global average rating across all movies

    /**
     * Calculate Bayesian Average (Weighted Rating)
     * Formula: WR = (v / (v + m)) * R + (m / (v + m)) * C
     * */
    public double calculateBayesianAverage(double avgRating, int voteCount) {
        if (voteCount == 0) {
            return globalMeanRating; // No votes = global average
        }

        // WR = (v / (v + m)) * R + (m / (v + m)) * C
        double weightedRating =
                ((double) voteCount / (voteCount + minimumVotes)) * avgRating +
                        ((double) minimumVotes / (voteCount + minimumVotes)) * globalMeanRating;

        return Math.round(weightedRating * 100.0) / 100.0; // Round to 2 decimals
    }

    @Cacheable(value = "movie-details", key = "#movieId")
    public MovieDTO getMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        return movieMapper.mapToDTO(movie);
    }

    @Transactional
    public MovieDTO syncToTmdb(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        return syncMovieFromTMDb(movie, movie.getTmdbId());
    }

    @Transactional
    public MovieDTO getOrFetchMovie(Long tmdbId) {
        Optional<Movie> existingMovie = movieRepository.findByTmdbId(tmdbId);

        if (existingMovie.isPresent()) {
            Movie movie = existingMovie.get();

            // Re-sync if data is old (older than 7 days)
            if (movie.getLastSyncedAt() == null ||
                    movie.getLastSyncedAt().isBefore(LocalDateTime.now().minusDays(7))) {
                log.info("Re-syncing movie data from TMDb: {}", tmdbId);
                return syncMovieFromTMDb(movie, tmdbId);
            }

            if(movie.getEmbedding() == null){
                embeddingService.generateMovieEmbedding(movie.getId());
                log.info("Embedding movie data movie from TMDb: {}", tmdbId);
            }

            return movieMapper.mapToDTO(movie);
        }

        // Fetch from TMDb and save
        log.info("Fetching new movie from TMDb: {}", tmdbId);
        MovieDTO tmdbMovie = tmDbClient.getMovieDetails(tmdbId);
        return saveMovieFromTMDb(tmdbMovie);
    }

    @Transactional
    public Page<MovieDTO> searchMovies(String query, Pageable pageable) {
        // Search local database first
        Page<Movie> localResults = movieRepository.searchByTitle(query, pageable);

        if (localResults.isEmpty()) {
            // If no local results, search TMDb
            log.info("No local results for query '{}', searching TMDb", query);
            List<MovieDTO> tmdbResults = tmDbClient.searchMovies(query, 1);

            // Save new movies to database
            tmdbResults.forEach(dto -> {
                if (movieRepository.findByTmdbId(dto.getTmdbId()).isEmpty()) {
                    MovieDTO movieDTO = saveMovieFromTMDb(dto);
                    fetchAndSaveKeywordsForMovie(movieDTO.getId());
                }
            });
        }

        return localResults.map(movieMapper::mapToDTO);
    }

    @Transactional
    public List<MovieDTO> getPopularMovies(int limit) {
        List<Movie> localResults = movieRepository.findPopularMovies(Pageable.ofSize(limit));

        if (localResults.isEmpty()) {
            log.info("No local results for popular movies... searching TMDb");
            List<MovieDTO> tmdbResults = tmDbClient.getPopularMovies(1);

            tmdbResults.forEach(dto -> {
                if (movieRepository.findByTmdbId(dto.getTmdbId()).isEmpty()) {
                    MovieDTO movieDTO = saveMovieFromTMDb(dto);
                    fetchAndSaveKeywordsForMovie(movieDTO.getId());
                }
            });
            localResults = movieRepository.findPopularMovies(Pageable.ofSize(limit));
        }

        return localResults.stream().map(movieMapper::mapToDTO).toList();
    }

    public List<MovieDTO> getMoviesByGenres(List<String> genreNames, int limit) {
        List<Movie> movies = movieRepository.findByGenreNames(genreNames,
                Pageable.ofSize(limit));
        return movies.stream()
                .map(movieMapper::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    protected MovieDTO saveMovieFromTMDb(MovieDTO dto) {
        Set<Genre> genres = new HashSet<>();
        Set<Keyword> keywords = new HashSet<>();
        Set<String> casts = new HashSet<>(dto.getCast());
        Set<String> directors = new HashSet<>(dto.getDirectors());


        // Handle genres
        if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
            genres = dto.getGenres().stream()
                    .map(genreDTO -> genreService.getOrCreateGenre(
                            genreDTO.getTmdbId(),
                            genreDTO.getName()
                    ))
                    .collect(Collectors.toSet());
        }

        // Handle keywords
        if (dto.getKeywords() != null && !dto.getKeywords().isEmpty()) {
            keywords = dto.getKeywords().stream()
                    .map(keywordDTO -> keywordService.getOrCreateKeyword(
                            keywordDTO.getTmdbId(),
                            keywordDTO.getName()
                    ))
                    .collect(Collectors.toSet());
        }

        Movie movie = Movie.builder()
                .tmdbId(dto.getTmdbId())
                .title(dto.getTitle())
                .originalTitle(dto.getOriginalTitle())
                .overview(dto.getOverview())
                .releaseDate(dto.getReleaseDate())
                .posterPath(dto.getPosterPath())
                .backdropPath(dto.getBackdropPath())
                .voteAverage(dto.getVoteAverage())
                .voteCount(dto.getVoteCount())
                .popularity(dto.getPopularity())
                .originalLanguage(dto.getOriginalLanguage())
                .genres(genres)
                .keywords(keywords)
                .cast(casts)
                .directors(directors)
                .runtime(dto.getRuntime())
                .lastSyncedAt(LocalDateTime.now())
                .build();


        movie = movieRepository.save(movie);

        // Update genre and keyword counts
        genres.forEach(genre -> genreService.incrementMovieCount(genre.getId()));
        genres.forEach(keyword -> keywordService.incrementMovieCount(keyword.getId()));

        log.info("Saved movie to database: {} (TMDb ID: {})", movie.getTitle(), movie.getTmdbId());

        return movieMapper.mapToDTO(movie);
    }

    @Transactional
    protected MovieDTO syncMovieFromTMDb(Movie movie, Long tmdbId) {
        MovieDTO tmdbData = tmDbClient.getMovieDetails(tmdbId);

        Set<String> casts = new HashSet<>(tmdbData.getCast());
        Set<String> directors = new HashSet<>(tmdbData.getDirectors());
        // Update basic info
        movie.setTitle(tmdbData.getTitle());
        movie.setOriginalTitle(tmdbData.getOriginalTitle());
        movie.setOriginalLanguage(tmdbData.getOriginalLanguage());
        movie.setOverview(tmdbData.getOverview());
        movie.setVoteAverage(tmdbData.getVoteAverage());
        movie.setVoteCount(tmdbData.getVoteCount());
        movie.setPopularity(tmdbData.getPopularity());
        movie.setLastSyncedAt(LocalDateTime.now());
        movie.setPosterPath(tmdbData.getPosterPath());
        movie.setBackdropPath(tmdbData.getBackdropPath());
        movie.setCast(casts);
        movie.setDirectors(directors);

        // Update genres
        Set<Genre> oldGenres = new HashSet<>(movie.getGenres());
        movie.getGenres().clear();

        Set<Genre> newGenres = new HashSet<>();

        if (tmdbData.getGenres() != null) {
            newGenres = tmdbData.getGenres().stream()
                    .map(genreDTO -> genreService.getOrCreateGenre(
                            genreDTO.getTmdbId(),
                            genreDTO.getName()
                    ))
                    .collect(Collectors.toSet());
        }

        // Update genre counts
        oldGenres.forEach(genre -> genreService.decrementMovieCount(genre.getId()));
        movie.getGenres().forEach(genre -> genreService.incrementMovieCount(genre.getId()));

        // Update keywords
        Set<Keyword> oldKeywords = new HashSet<>(movie.getKeywords());
        movie.getKeywords().clear();

        Set<Keyword> newKeywords = new HashSet<>();

        if (tmdbData.getKeywords() != null) {
            newKeywords = tmdbData.getKeywords().stream()
                    .map(keywordDTO -> keywordService.getOrCreateKeyword(
                            keywordDTO.getTmdbId(),
                            keywordDTO.getName()
                    ))
                    .collect(Collectors.toSet());
        }

        movie.setKeywords(newKeywords);
        movie.setGenres(newGenres);

        // Update keyword counts
        oldKeywords.forEach(keyword -> keywordService.decrementMovieCount(keyword.getId()));
        movie.getKeywords().forEach(keyword -> keywordService.incrementMovieCount(keyword.getId()));

        movie = movieRepository.save(movie);
        return movieMapper.mapToDTO(movie);
    }

    @Transactional
    public String getMoviesByGenre(String genreName, int limit) {
        log.info("Getting movies for genre: {}", genreName);

        Integer genreId = genreService.findByName(genreName).get().getTmdbId();

        // Not enough movies in DB, fetch from TMDb
        log.info("Fetching movies from TMDb for genre: {} (ID: {})", genreName, genreId);

        // Fetch multiple pages to ensure we get enough movies
        List<MovieDTO> allTmdbMovies = new ArrayList<>();
        int pagesToFetch = Math.min(3, (limit / 20) + 1); // Fetch up to 3 pages

        for (int page = 1; page <= 8; page++) {
            try {
                List<MovieDTO> tmdbMovies = tmDbClient.getMoviesByGenre(genreId, page);
                allTmdbMovies.addAll(tmdbMovies);

                // Save each movie to database if it doesn't exist
                for (MovieDTO dto : tmdbMovies) {
                    if (movieRepository.findByTmdbId(dto.getTmdbId()).isEmpty()) {
                        saveMovieFromTMDb(dto);
                        log.debug("Saved movie to DB: {} (TMDb ID: {})", dto.getTitle(), dto.getTmdbId());
                    }
                }

                // Stop if we have enough movies
                if (allTmdbMovies.size() >= limit) {
                    break;
                }
            } catch (Exception e) {
                log.error("Error fetching page {} for genre {}: {}", page, genreName, e.getMessage());
                break;
            }
        }
        return "Successfully fetched " + allTmdbMovies.size() + " movies";
    }


    @Transactional
    public void fetchAndSaveKeywordsForMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + movieId));

        Long tmdbId = movie.getTmdbId();
        log.info("Fetching keywords for movie: {} (TMDb ID: {})", movie.getTitle(), tmdbId);

        // Get keywords from TMDb
        List<KeywordDTO> keywordDTOs = tmDbClient.getMovieKeywords(tmdbId);

        if (keywordDTOs.isEmpty()) {
            log.info("No keywords found for movie: {}", movie.getTitle());
            return;
        }

        // Clear existing keywords counts
        Set<Keyword> oldKeywords = new HashSet<>(movie.getKeywords());
        movie.getKeywords().clear();

        Set<Keyword> newKeywords = new HashSet<>();

        // Create/get keyword entities and link to movie
        newKeywords = keywordDTOs.stream()
                .map(keywordDTO -> keywordService.getOrCreateKeyword(
                        keywordDTO.getTmdbId(),
                        keywordDTO.getName()
                ))
                .collect(Collectors.toSet());

        movie.setKeywords(newKeywords);

        movieRepository.save(movie);

        // Update keyword counts
        oldKeywords.forEach(keyword -> keywordService.decrementMovieCount(keyword.getId()));
        newKeywords.forEach(keyword -> keywordService.incrementMovieCount(keyword.getId()));

        log.info("Successfully saved {} keywords for movie: {}", newKeywords.size(), movie.getTitle());
    }

    /**
     * Batch fetch and save keywords for multiple movies
     */
    @Transactional
    public void fetchAndSaveKeywordsForMovies(List<Long> movieIds) {
        log.info("Batch fetching keywords for {} movies", movieIds.size());

        int successCount = 0;
        int failCount = 0;

        for (Long movieId : movieIds) {
            try {
                fetchAndSaveKeywordsForMovie(movieId);
                successCount++;

                // Add delay to respect rate limits
                Thread.sleep(250);

            } catch (Exception e) {
                log.error("Error fetching keywords for movie {}: {}", movieId, e.getMessage());
                failCount++;
            }
        }

        log.info("Batch keyword fetch completed: {} successful, {} failed", successCount, failCount);
    }
}
