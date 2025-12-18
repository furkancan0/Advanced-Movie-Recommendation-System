CREATE TABLE chat_conversations (
        id BIGSERIAL PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE,
        title VARCHAR(255) NOT NULL DEFAULT 'Movie Mentor',
        message_count INTEGER DEFAULT 0,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_chat_conversations_user ON chat_conversations(user_id);
CREATE INDEX idx_chat_conversations_created ON chat_conversations(created_at DESC);

CREATE TABLE chat_messages (
       id BIGSERIAL PRIMARY KEY,
       conversation_id BIGINT NOT NULL,
       role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
       content TEXT NOT NULL,
       is_off_topic BOOLEAN DEFAULT FALSE,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages(conversation_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(created_at ASC);
CREATE INDEX idx_chat_messages_role ON chat_messages(role);

-- Create message_movie_refs table (movies referenced in assistant responses)
CREATE TABLE message_movie_refs (
        message_id BIGINT NOT NULL,
        movie_id BIGINT NOT NULL,
        PRIMARY KEY (message_id, movie_id),
        FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
        FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

CREATE INDEX idx_message_movie_refs_message ON message_movie_refs(message_id);
CREATE INDEX idx_message_movie_refs_movie ON message_movie_refs(movie_id);

CREATE INDEX idx_chat_messages_conversation_created
    ON chat_messages(conversation_id, created_at ASC);

-- Index for message count
CREATE INDEX idx_chat_conversations_message_count
    ON chat_conversations(message_count DESC);