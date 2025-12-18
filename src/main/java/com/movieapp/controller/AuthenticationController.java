package com.movieapp.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.movieapp.dto.AuthenticationRequest;
import com.movieapp.dto.AuthenticationResponse;
import com.movieapp.dto.GoogleOAuthRequest;
import com.movieapp.dto.RegisterRequest;
import com.movieapp.service.AuthenticationService;
import com.movieapp.util.GoogleTokenVerifier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> googleAuth(
            @Valid @RequestBody GoogleOAuthRequest request) {
        AuthenticationResponse response = authenticationService.handleGoogleAuth(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestHeader("Authorization") String refreshToken) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshToken));
    }
}
