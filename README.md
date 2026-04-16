# Advanced Movie Recommendation System

### Dual AI-Powered Recommendations
- **Genre-Based Engine**: Collaborative + content-based filtering using user ratings
- **Vector-Based Engine**: Semantic similarity using 768-dim embeddings (Ollama)

### Core Features
- **Movie Discovery**: Search 1000+ movies with TMDb integration
- **Smart Rating System**: 5-star ratings with automatic recommendation updates
- **Bookmarking**: Save movies to watch later
- **Vector Similarity Search**: Find movies by semantic meaning
- **Rate Limiting**: Protect API with Bucket4j
- **JWT Authentication**: Secure with role-based access (USER, MODERATOR, ADMIN)
- **Google Authentication**: Basic google auth
- **Caching**: (Caffeine): In-memory, 1-hour TTL

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React/Vue/etc)                  │
└───────────────────────┬─────────────────────────────────────┘
                        │ REST API + JWT
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Application                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  Controllers │  │   Services   │  │ Repositories │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
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
Variables  
Create a `.env` file or export:  

TMDB_API_KEY=your_tmdb_api_key  
JWT_SECRET_KEY=$(openssl rand -base64 32)  
DB_URL=jdbc:postgresql://localhost:5432/moviedb  
DB_USERNAME=postgres  
DB_PASSWORD=your_password  
OLLAMA_BASE_URL=http://localhost:11434  
GOOGLE_CLIENT_ID=your_google_client_id  
GOOGLE_CLIENT_SECRET=your_google_client_secret  
