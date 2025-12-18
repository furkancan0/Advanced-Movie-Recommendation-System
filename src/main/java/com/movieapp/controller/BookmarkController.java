package com.movieapp.controller;

import com.movieapp.dto.MovieDTO;
import com.movieapp.entity.User;
import com.movieapp.service.BookmarkService;
import com.movieapp.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final RateLimiter rateLimiter;

    /**
     * Add a bookmark - REQUIRES AUTHENTICATION
     */
    @PostMapping("/movie/{movieId}")
    public ResponseEntity<Void> addBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable Long movieId) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        bookmarkService.addBookmark(user.getId(), movieId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Remove a bookmark - REQUIRES AUTHENTICATION
     */
    @DeleteMapping("/movie/{movieId}")
    public ResponseEntity<Void> removeBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable Long movieId) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        bookmarkService.removeBookmark(user.getId(), movieId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all bookmarks for current user - REQUIRES AUTHENTICATION
     */
    @GetMapping
    public ResponseEntity<Page<MovieDTO>> getMyBookmarks(
            @AuthenticationPrincipal User user,
            Pageable pageable) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        Page<MovieDTO> bookmarks = bookmarkService.getUserBookmarks(user.getId(), pageable);
        return ResponseEntity.ok(bookmarks);
    }

    /**
     * Check if a movie is bookmarked - REQUIRES AUTHENTICATION
     */
    @GetMapping("/movie/{movieId}/check")
    public ResponseEntity<Boolean> checkBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable Long movieId) {

        rateLimiter.checkRateLimit("user:" + user.getId());

        boolean isBookmarked = bookmarkService.isBookmarked(user.getId(), movieId);
        return ResponseEntity.ok(isBookmarked);
    }
}