# 🎬 Advanced Movie Recommendation System

A production-ready, full-stack movie recommendation platform powered by **Spring Boot**, **PostgreSQL**, **pgvector**, and **Ollama AI**. Features dual recommendation engines, RAG-powered chat assistant, and comprehensive movie discovery tools.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🌟 Key Features

### 🤖 Dual AI-Powered Recommendations
- **Genre-Based Engine**: Collaborative + content-based filtering using user ratings
- **Vector-Based Engine**: Semantic similarity using 768-dim embeddings (Ollama)
- Automatic cold-start handling with popular movies
- Real-time preference learning from user ratings

### 💬 RAG-Powered Movie Mentor
- Natural language movie queries with context-aware responses
- Single persistent conversation per user (mentor-style)
- Semantic search using vector embeddings
- Automatic filter extraction from natural language
- Topic validation (rejects non-movie queries)
- Full conversation history with context retention

### 🎯 Core Features
- **Movie Discovery**: Search 1000+ movies with TMDb integration
- **Smart Rating System**: 5-star ratings with automatic recommendation updates
- **Bookmarking**: Save movies to watch later
- **Vector Similarity Search**: Find movies by semantic meaning
- **Multi-layer Caching**: Caffeine + Redis for optimal performance
- **Rate Limiting**: Protect API with Bucket4j
- **JWT Authentication**: Secure with role-based access (USER, MODERATOR, ADMIN)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React/Vue/etc)                  │
└───────────────────────┬─────────────────────────────────────┘
                        │ REST API + JWT
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Application                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Controllers │  │   Services   │  │ Repositories │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ PostgreSQL   │ │    Ollama    │ │   TMDb API   │
│ + pgvector   │ │   llama3.2   │ │              │
│              │ │ nomic-embed  │ │              │
└──────────────┘ └──────────────┘ └──────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- [Ollama](https://ollama.com) (for AI features)
- TMDb API Key ([Get one here](https://www.themoviedb.org/settings/api))

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/movie-recommendation-system.git
cd movie-recommendation-system
```

### 2. Install Ollama & Pull Models

```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Pull required models
ollama pull llama3.2          # Chat model (~2GB)
ollama pull nomic-embed-text  # Embedding model (~275MB)

# Start Ollama server
ollama serve
```

### 3. Setup PostgreSQL Database

```bash
# Create database
createdb moviedb

# Enable pgvector extension
psql -d moviedb -c "CREATE EXTENSION vector;"
```

### 4. Configure Environment Variables

Create a `.env` file or export:

```bash
export TMDB_API_KEY=your_tmdb_api_key_here
export JWT_SECRET_KEY=$(openssl rand -base64 32)
export DB_URL=jdbc:postgresql://localhost:5432/moviedb
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export OLLAMA_BASE_URL=http://localhost:11434
```

### 5. Build & Run

```bash
# Build
mvn clean install

# Run migrations
mvn flyway:migrate

# Start application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 6. Seed Database with Movies (Optional)

```bash
# Seed 1000 movies from TMDb
curl -X POST http://localhost:8080/api/admin/seeder/seed \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{"totalMovies": 1000}'
```

## 📖 API Documentation

### Authentication

#### Register
```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "furkan",
  "email": "furkan@example.com",
  "password": "furkansecur123"
}
```

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123"
}

Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "userId": 1,
  "username": "furkan",
  "role": "USER"
}
```

### Movie Operations

#### Search Movies
```bash
GET /api/movies/search?query=inception&page=0&size=20
```

#### Get Movie Details
```bash
GET /api/movies/{movieId}
Authorization: Bearer {token}
```

#### Find Similar Movies (Vector Search)
```bash
GET /api/movies/{movieId}/similar?limit=10
```

### Recommendations

#### Genre-Based Recommendations
```bash
GET /api/recommendations/genre-based?limit=20
Authorization: Bearer {token}
```

#### Vector-Based Recommendations (AI-Powered)
```bash
GET /api/recommendations/vector-based?limit=20
Authorization: Bearer {token}
```

#### Get Both Types
```bash
GET /api/recommendations/dual?limit=20
Authorization: Bearer {token}

Response:
{
  "genreBasedRecommendations": [...],
  "vectorBasedRecommendations": [
    {
      "movieId": 550,
      "title": "Fight Club",
      "similarity": 0.89,
      "avgRating": 4.5
    }
  ]
}
```

### Chat with Movie Mentor

#### Send Message
```bash
POST /api/chat/send
Authorization: Bearer {token}
Content-Type: application/json

{
  "message": "Recommend movies like Interstellar and explain why"
}

Response:
{
  "messageId": 42,
  "response": "Since you loved Interstellar's exploration of time and space...",
  "suggestedMovies": [...],
  "totalMessages": 10,
  "timestamp": "2024-01-15T10:30:00"
}
```

#### Get Chat History
```bash
GET /api/chat/history
Authorization: Bearer {token}
```

#### Clear Chat History
```bash
DELETE /api/chat/history
Authorization: Bearer {token}
```

### Ratings & Bookmarks

#### Rate a Movie
```bash
POST /api/ratings
Authorization: Bearer {token}
Content-Type: application/json

{
  "movieId": 550,
  "rating": 5,
  "review": "Masterpiece!"
}
```

#### Add Bookmark
```bash
POST /api/bookmarks/movie/{movieId}
Authorization: Bearer {token}
```

## 🔧 Configuration

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/moviedb
    username: postgres
    password: password

jwt:
  secret-key: ${JWT_SECRET_KEY}
  expiration: 86400000  # 24 hours

tmdb:
  api:
    key: ${TMDB_API_KEY}

ollama:
  base-url: http://localhost:11434
  chat-model: llama3.2
  model: nomic-embed-text
  embedding-dimension: 768

cache:
  type: caffeine  # or redis for distributed
```

## 📊 Database Schema

### Core Tables
- `users` - User accounts with JWT authentication
- `movies` - Movie metadata with vector embeddings
- `genres` - Movie genres (normalized)
- `keywords` - Movie keywords/themes
- `ratings` - User ratings (1-5 stars)
- `bookmarks` - User's saved movies
- `chat_conversations` - One mentor conversation per user
- `chat_messages` - Chat message history

### Key Features
- **Vector columns**: `movies.embedding vector(768)`, `users.preference_vector vector(768)`
- **HNSW indexes**: Fast similarity search (10ms vs 10s)
- **Full-text search**: pg_trgm for movie titles
- **Optimistic locking**: Prevent concurrent update issues

## 🧠 How It Works

### Recommendation Engines

#### 1. Genre-Based (Collaborative + Content-Based)
```
User rates movies → Extract genre preferences → 
Find similar users (Pearson correlation) → 
Recommend movies they loved
```

**Weights:**
- 5-star rating = 2.0
- 4-star rating = 1.5
- 3-star rating = 1.0
- 1-2 star rating = 0.5

#### 2. Vector-Based (Semantic Similarity)
```
Movie → Generate embedding (overview + genres + keywords) →
Store in PostgreSQL (768-dim vector) →
User rates movies → Create user preference vector →
KNN search for similar movies
```

### RAG Chat Pipeline

```
User Query: "Show me war movies that end dramatically, rated 4+"
    ↓
1. Topic Validation (Is this about movies?)
    ↓
2. Extract Filters (genre=war, minRating=4)
    ↓
3. Semantic Search (Vector similarity)
    ↓
4. Build Context (Retrieved movies + chat history)
    ↓
5. LLM Generation (Ollama llama3.2)
    ↓
Response: "Here are excellent war movies with dramatic endings..."
```

## 🎯 Performance

### Caching Strategy
- **L1 Cache** (Caffeine): In-memory, 1-hour TTL
- **Cached Data**: Movie details, recommendations, vector search results

## 🔒 Security

### Authentication
- JWT tokens (24-hour access, 7-day refresh)
- BCrypt password hashing
- Role-based access control (USER, MODERATOR, ADMIN)

### Rate Limiting
- 100 requests/minute per user (default)
- 20 requests/minute for chat
- TMDb API: 40 requests/10 seconds (auto-managed)

### Data Protection
- SQL injection prevention (JPA/Hibernate)
- Input validation (Jakarta Validation)
- CORS configuration for production

## 🐳 Docker Deployment

### Using Docker Compose

```bash
docker-compose up -d
```

## 📈 Monitoring

### Actuator Endpoints
- `/actuator/health` - Application health
- `/actuator/metrics` - Performance metrics
- `/actuator/caches` - Cache statistics

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some MovieFeature'`)
4. Push to the branch (`git push origin feature/MovieFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [TMDb](https://www.themoviedb.org/) - Movie data API
- [Ollama](https://ollama.com/) - Local LLM inference
- [pgvector](https://github.com/pgvector/pgvector) - Vector similarity search
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- Inspired by [MovieLens](https://movielens.org/) and [JustWatch](https://www.justwatch.com/)


**Built with ❤️ using Spring Boot, PostgreSQL, and Ollama**
