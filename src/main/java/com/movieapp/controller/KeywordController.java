package com.movieapp.controller;

import com.movieapp.dto.KeywordDTO;
import com.movieapp.service.KeywordService;
import com.movieapp.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;
    private final RateLimiter rateLimiter;

    /**
     * Get popular keywords
     */
    @GetMapping("/popular")
    public ResponseEntity<List<KeywordDTO>> getPopularKeywords(
            @RequestParam(defaultValue = "50") int limit) {

        rateLimiter.checkRateLimit("anonymous");

        List<KeywordDTO> keywords = keywordService.getPopularKeywords(limit);
        return ResponseEntity.ok(keywords);
    }

    /**
     * Get keyword by ID
     */
    @GetMapping("/{keywordId}")
    public ResponseEntity<KeywordDTO> getKeywordById(@PathVariable Long keywordId) {
        KeywordDTO keyword = keywordService.getKeywordById(keywordId);
        return ResponseEntity.ok(keyword);
    }
}