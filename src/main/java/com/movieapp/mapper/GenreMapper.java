package com.movieapp.mapper;

import com.movieapp.dto.GenreDTO;
import com.movieapp.entity.Genre;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GenreMapper {
    public GenreDTO mapToDTO(Genre genre) {
        return GenreDTO.builder()
                .id(genre.getId())
                .tmdbId(genre.getTmdbId())
                .name(genre.getName())
                .description(genre.getDescription())
                .movieCount(genre.getMovieCount())
                .build();
    }

    public List<GenreDTO> toGenreDTOList(Set<Genre> genres) {
        if (genres == null) return new ArrayList<>();
        return genres.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
