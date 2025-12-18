package com.movieapp.service;

import com.movieapp.dto.AuthenticationResponse;
import com.movieapp.dto.GoogleOAuthRequest;
import com.movieapp.dto.RegisterRequest;
import com.movieapp.dto.AuthenticationRequest;
import com.movieapp.entity.Role;
import com.movieapp.entity.User;
import com.movieapp.repository.UserRepository;
import com.movieapp.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .accountLocked(false)
                .onboardingCompleted(false)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .needsOnboarding(!user.getOnboardingCompleted())
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User authenticated: {}", user.getUsername());

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .needsOnboarding(!user.getOnboardingCompleted())
                .build();
    }

    @Transactional
    public AuthenticationResponse handleGoogleAuth(GoogleOAuthRequest request) {
        // Check if user exists by Google ID
        Optional<User> existingUser = userRepository.findByGoogleId(request.getGoogleId());

        User user;
        if (existingUser.isPresent()) {
            // Existing Google user
            user = existingUser.get();
            log.info("Existing Google user logged in: {}", user.getUsername());
        } else {
            // Check if email already exists (user might have registered with email/password)
            Optional<User> emailUser = userRepository.findByEmail(request.getEmail());

            if (emailUser.isPresent()) {
                user = emailUser.get();
                user.setGoogleId(request.getGoogleId());
                user.setOauthProvider("google");
                user = userRepository.save(user);
                log.info("Linked Google account to existing user: {}", user.getUsername());
            } else {
                // Create new user from Google data
                String username = generateUsernameFromEmail(request.getEmail());

                user = User.builder()
                        .username(username)
                        .email(request.getEmail())
                        .googleId(request.getGoogleId())
                        .role(Role.USER)
                        .enabled(true)
                        .accountLocked(false)
                        .onboardingCompleted(false)
                        .oauthProvider("google")
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                        .build();

                user = userRepository.save(user);
                log.info("New Google user registered: {}", user.getUsername());
            }
        }

        return buildAuthResponse(user);
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .needsOnboarding(!user.getOnboardingCompleted())
                .build();
    }

    private AuthenticationResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .needsOnboarding(!user.getOnboardingCompleted())
                .build();
    }

    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
