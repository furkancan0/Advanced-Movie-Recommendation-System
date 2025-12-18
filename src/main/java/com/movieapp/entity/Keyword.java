package com.movieapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "keywords", indexes = {
        @Index(name = "idx_keyword_tmdb_id", columnList = "tmdb_id"),
        @Index(name = "idx_keyword_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"movies"})
@ToString(exclude = {"movies"})
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", unique = true, nullable = false)
    private Integer tmdbId;

    @Column(nullable = false)
    private String name;

    @ManyToMany(mappedBy = "keywords")
    private Set<Movie> movies = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "movie_count")
    private Integer movieCount = 0;
}