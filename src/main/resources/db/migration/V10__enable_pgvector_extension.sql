-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding column to movies table
ALTER TABLE movies
    ADD COLUMN IF NOT EXISTS embedding vector(768),
    ADD COLUMN IF NOT EXISTS embedding_generated_at TIMESTAMP;

-- Create index for vector similarity search (HNSW - faster for large datasets)
CREATE INDEX IF NOT EXISTS idx_movies_embedding_hnsw
    ON movies USING hnsw (embedding vector_cosine_ops);


-- Add user preference vector
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS preference_vector vector(768),
    ADD COLUMN IF NOT EXISTS preference_vector_updated_at TIMESTAMP;

-- Create index for user vector search
CREATE INDEX IF NOT EXISTS idx_users_preference_vector_hnsw
    ON users USING hnsw (preference_vector vector_cosine_ops);

-- Create additional indexes for better query performance

-- Index for finding movies without embeddings (for batch processing)
CREATE INDEX IF NOT EXISTS idx_movies_null_embedding
    ON movies(id) WHERE embedding IS NULL;

-- Index for finding movies with old embeddings
CREATE INDEX IF NOT EXISTS idx_movies_old_embedding
    ON movies(embedding_generated_at) WHERE embedding_generated_at IS NOT NULL;

-- Composite index for movie search with embeddings
CREATE INDEX IF NOT EXISTS idx_movies_title_with_embedding
    ON movies(title) WHERE embedding IS NOT NULL;

-- Statistics for better query planning
ANALYZE movies;
ANALYZE users;