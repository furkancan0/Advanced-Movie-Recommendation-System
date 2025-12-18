CREATE TABLE movies (
    id BIGSERIAL PRIMARY KEY,
    tmdb_id BIGINT UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    original_title VARCHAR(500),
    overview TEXT,
    release_date DATE,
    poster_path VARCHAR(500),
    backdrop_path VARCHAR(500),
    vote_average DOUBLE PRECISION,
    vote_count INTEGER,
    popularity DOUBLE PRECISION,
    original_language VARCHAR(10),
    runtime INTEGER,
    avg_rating DOUBLE PRECISION,
    rating_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_movies_tmdb_id ON movies(tmdb_id);
CREATE INDEX idx_movies_title ON movies(title);
CREATE INDEX idx_movies_popularity ON movies(popularity DESC);
CREATE INDEX idx_movies_avg_rating ON movies(avg_rating DESC);

CREATE TABLE movie_cast (
    movie_id BIGINT NOT NULL,
    actor VARCHAR(255) NOT NULL,
    PRIMARY KEY (movie_id, actor),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

CREATE TABLE movie_directors (
     movie_id BIGINT NOT NULL,
     director VARCHAR(255) NOT NULL,
     PRIMARY KEY (movie_id, director),
     FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);