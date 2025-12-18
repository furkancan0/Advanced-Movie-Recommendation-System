package com.movieapp.dto;

import lombok.Data;

@Data
public class RatingRequest {
    private Long movieId;
    private Integer rating;
    private String review;
}
