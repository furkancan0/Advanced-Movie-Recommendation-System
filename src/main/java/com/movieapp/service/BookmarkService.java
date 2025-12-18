package com.movieapp.service;

import com.movieapp.dto.MovieDTO;
import com.movieapp.entity.Bookmark;
import com.movieapp.entity.Movie;
import com.movieapp.entity.User;
import com.movieapp.mapper.MovieMapper;
import com.movieapp.repository.BookmarkRepository;
import com.movieapp.repository.MovieRepository;
import com.movieapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    private final MovieMapper movieMapper;

    @Transactional
    public void addBookmark(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (bookmarkRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new RuntimeException("Movie already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .movie(movie)
                .build();

        bookmarkRepository.save(bookmark);
        log.info("User {} bookmarked movie {}", userId, movieId);
    }

    @Transactional
    public void removeBookmark(Long userId, Long movieId) {
        if (!bookmarkRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new RuntimeException("Bookmark not found");
        }

        bookmarkRepository.deleteByUserIdAndMovieId(userId, movieId);
        log.info("User {} removed bookmark for movie {}", userId, movieId);
    }

    public boolean isBookmarked(Long userId, Long movieId) {
        return bookmarkRepository.existsByUserIdAndMovieId(userId, movieId);
    }

    public Page<MovieDTO> getUserBookmarks(Long userId, Pageable pageable) {
        Page<Bookmark> bookmarks = bookmarkRepository.findByUserId(userId, pageable);
        return bookmarks.map(bookmark -> movieMapper.mapToDTO(bookmark.getMovie()));
    }
}
