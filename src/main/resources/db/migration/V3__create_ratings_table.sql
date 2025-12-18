CREATE TABLE ratings (
     id BIGSERIAL PRIMARY KEY,
     user_id BIGINT NOT NULL,
     movie_id BIGINT NOT NULL,
     rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
     review TEXT,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     version BIGINT DEFAULT 0,
     UNIQUE (user_id, movie_id),
     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
     FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

CREATE INDEX idx_ratings_user ON ratings(user_id);
CREATE INDEX idx_ratings_movie ON ratings(movie_id);
CREATE INDEX idx_ratings_rating ON ratings(rating);
CREATE INDEX idx_ratings_created_at ON ratings(created_at DESC);