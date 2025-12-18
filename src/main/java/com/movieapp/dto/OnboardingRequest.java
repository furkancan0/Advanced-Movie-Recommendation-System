package com.movieapp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingRequest {
    @NotEmpty(message = "Please select at least 3 favorite genres")
    @Size(min = 3, message = "Please select at least 3 genres")
    private List<String> favoriteGenres;
}
