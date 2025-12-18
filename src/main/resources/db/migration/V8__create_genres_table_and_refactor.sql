-- Create genres table
CREATE TABLE genres (
        id BIGSERIAL PRIMARY KEY,
        tmdb_id INTEGER UNIQUE NOT NULL,
        name VARCHAR(100) UNIQUE NOT NULL,
        description TEXT,
        movie_count INTEGER DEFAULT 0,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_genre_tmdb_id ON genres(tmdb_id);
CREATE INDEX idx_genre_name ON genres(name);
CREATE INDEX idx_genre_movie_count ON genres(movie_count DESC);

-- Create keywords table
CREATE TABLE keywords (
      id BIGSERIAL PRIMARY KEY,
      tmdb_id INTEGER UNIQUE NOT NULL,
      name VARCHAR(255) NOT NULL,
      movie_count INTEGER DEFAULT 0,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_keyword_tmdb_id ON keywords(tmdb_id);
CREATE INDEX idx_keyword_name ON keywords(name);
CREATE INDEX idx_keyword_movie_count ON keywords(movie_count DESC);

-- Drop old movie_genres ElementCollection table
DROP TABLE IF EXISTS movie_genres CASCADE;

-- Create new movie_genres junction table for ManyToMany relationship
CREATE TABLE movie_genres (
      movie_id BIGINT NOT NULL,
      genre_id BIGINT NOT NULL,
      PRIMARY KEY (movie_id, genre_id),
      FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
      FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

CREATE INDEX idx_movie_genres_movie ON movie_genres(movie_id);
CREATE INDEX idx_movie_genres_genre ON movie_genres(genre_id);

-- Create movie_keywords junction table
CREATE TABLE movie_keywords (
        movie_id BIGINT NOT NULL,
        keyword_id BIGINT NOT NULL,
        PRIMARY KEY (movie_id, keyword_id),
        FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
        FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE
);

CREATE INDEX idx_movie_keywords_movie ON movie_keywords(movie_id);
CREATE INDEX idx_movie_keywords_keyword ON movie_keywords(keyword_id);

-- Insert standard TMDb genres
INSERT INTO genres (tmdb_id, name, description) VALUES
        (28, 'Action', 'Movies with intense sequences and physical stunts'),
        (12, 'Adventure', 'Exciting stories with exotic locations and quests'),
        (16, 'Animation', 'Animated films and cartoons'),
        (35, 'Comedy', 'Funny and humorous films'),
        (80, 'Crime', 'Crime and detective stories'),
        (99, 'Documentary', 'Non-fiction films based on real events'),
        (18, 'Drama', 'Serious, plot-driven films'),
        (10751, 'Family', 'Films suitable for all ages'),
        (14, 'Fantasy', 'Films with magical and supernatural elements'),
        (36, 'History', 'Historical events and periods'),
        (27, 'Horror', 'Scary and suspenseful films'),
        (10402, 'Music', 'Musical films and concerts'),
        (9648, 'Mystery', 'Films with puzzles and secrets to solve'),
        (10749, 'Romance', 'Love stories and romantic dramas'),
        (878, 'Science Fiction', 'Futuristic and sci-fi themes'),
        (10770, 'TV Movie', 'Made-for-television movies'),
        (53, 'Thriller', 'Suspenseful and tense films'),
        (10752, 'War', 'War and military conflicts'),
        (37, 'Western', 'Films set in the American Old West')
    ON CONFLICT (tmdb_id) DO NOTHING;