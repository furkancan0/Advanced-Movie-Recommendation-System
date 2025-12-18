package com.movieapp.dto;

import lombok.Data;

@Data
public class GoogleOAuthRequest {
    private String email;
    private String name;
    private String googleId;
}
