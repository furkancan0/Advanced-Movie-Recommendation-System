CREATE TABLE bookmarks (
       id BIGSERIAL PRIMARY KEY,
       user_id BIGINT NOT NULL,
       movie_id BIGINT NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       UNIQUE (user_id, movie_id),
       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
       FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

CREATE INDEX idx_bookmarks_user ON bookmarks(user_id);
CREATE INDEX idx_bookmarks_movie ON bookmarks(movie_id);
CREATE INDEX idx_bookmarks_created_at ON bookmarks(created_at DESC);