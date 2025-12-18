-- Add GIN index for full-text search on genre names
CREATE INDEX idx_genres_name_gin ON genres USING gin(to_tsvector('english', name));

-- Add GIN index for full-text search on keyword names
CREATE INDEX idx_keywords_name_gin ON keywords USING gin(to_tsvector('english', name));

-- Add composite index for movie genre queries
CREATE INDEX idx_movie_genres_composite ON movie_genres(genre_id, movie_id);

-- Add composite index for movie keyword queries
CREATE INDEX idx_movie_keywords_composite ON movie_keywords(keyword_id, movie_id);

-- Add index for movies by popularity within genre
CREATE INDEX idx_movies_genre_popularity ON movies(popularity DESC) WHERE popularity IS NOT NULL;

-- Add index for movies by rating within genre
CREATE INDEX idx_movies_genre_rating ON movies(avg_rating DESC, rating_count DESC)
    WHERE avg_rating IS NOT NULL AND rating_count > 0;