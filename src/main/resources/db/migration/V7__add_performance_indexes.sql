-- Enable trigram extension for better text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_movies_title_trgm ON movies USING gin (title gin_trgm_ops);

-- Composite indexes for common queries
CREATE INDEX idx_ratings_user_created ON ratings(user_id, created_at DESC);
CREATE INDEX idx_ratings_movie_rating ON ratings(movie_id, rating DESC);

-- Partial indexes for commonly filtered data
CREATE INDEX idx_movies_recent ON movies(release_date DESC)
    WHERE release_date IS NOT NULL;

CREATE INDEX idx_movies_highly_rated ON movies(avg_rating DESC)
    WHERE avg_rating IS NOT NULL AND rating_count >= 10;

CREATE INDEX idx_movies_popular_recent ON movies(popularity DESC, release_date DESC)
    WHERE popularity > 50;

CREATE INDEX idx_movies_for_onboarding ON movies(popularity DESC, vote_average DESC)
    WHERE popularity > 100 AND vote_average > 7.0;