package com.movieapp.service;

import com.movieapp.dto.ChatMessageDTO;
import com.movieapp.dto.MovieDTO;
import com.movieapp.dto.VectorSimilarityResult;
import com.movieapp.entity.*;
import com.movieapp.repository.ChatConversationRepository;
import com.movieapp.repository.ChatMessageRepository;
import com.movieapp.repository.MovieRepository;
import com.movieapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Service
@Slf4j
@RequiredArgsConstructor
public class MovieChatService {

    private final OllamaLLMService llmService;
    private final VectorSearchService vectorSearchService;
    private final MovieRepository movieRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    private static final int MAX_CONTEXT_MESSAGES = 10;
    private static final int MAX_RETRIEVED_MOVIES = 20;

    /**
     * Main chat endpoint with RAG pipeline - Single conversation per user
     */
    @Transactional
    public ChatMessageDTO chat(Long userId, String userMessage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Processing chat for user {}: {}", userId, userMessage);

        // Step 1: Check if query is about movies (topic validation)
        if (!isMovieRelatedQuery(userMessage)) {
            log.info("Off-topic query detected: {}", userMessage);
            return buildOffTopicResponse();
        }

        // Step 2: Get or create user's single conversation
        ChatConversation conversation = getOrCreateUserConversation(user);

        // Step 3: Save user message
        ChatMessage userMsg = saveMessage(conversation, MessageRole.USER, userMessage, false);

        // Step 4: Retrieve relevant movies (RAG retrieval)
        List<Movie> relevantMovies = retrieveRelevantMovies(userMessage);

        // Step 5: Build context with conversation history and retrieved movies
        String context = buildContext(conversation, relevantMovies);

        // Step 6: Generate response using LLM
        String assistantResponse = generateResponse(conversation, userMessage, context);

        // Step 7: Extract movie IDs from response
        List<Long> referencedMovieIds = extractMovieIds(assistantResponse, relevantMovies);

        // Step 8: Save assistant message
        ChatMessage assistantMsg = saveMessage(conversation, MessageRole.ASSISTANT,
                assistantResponse, false);
        assistantMsg.setReferencedMovieIds(referencedMovieIds);
        messageRepository.save(assistantMsg);

        // Step 9: Update message count
        //conversation.setMessageCount(conversation.getMessages().size());
        conversationRepository.save(conversation);

        // Step 10: Build response with suggested movies
        List<MovieDTO> suggestedMovies = relevantMovies.stream()
                .limit(10)
                .map(this::mapToMovieDTO)
                .collect(Collectors.toList());

        return ChatMessageDTO.builder()
                .id(assistantMsg.getId())
                .content(assistantResponse)
                .isOffTopic(false)
                .role(MessageRole.ASSISTANT)
                //.suggestedMovies(suggestedMovies)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Check if query is related to movies
     */
    private boolean isMovieRelatedQuery(String query) {
        String lowerQuery = query.toLowerCase();

        // Movie-related keywords
        String[] movieKeywords = {
                "movie", "film", "watch", "recommend", "show me", "suggest",
                "actor", "actress", "director", "genre", "drama", "action",
                "comedy", "thriller", "horror", "romance", "sci-fi", "animation",
                "plot", "story", "character", "scene", "ending", "rated"
        };

        // Check if query contains movie keywords
        for (String keyword : movieKeywords) {
            if (lowerQuery.contains(keyword)) {
                return true;
            }
        }

        // Use LLM to determine if it's movie-related (fallback)
        String prompt = "Is the following question related to movies, films, or cinema? " +
                "Answer with only 'yes' or 'no'.\n\nQuestion: " + query;

        String response = llmService.chat(prompt, null);
        return response.toLowerCase().trim().startsWith("yes");
    }

    /**
     * Retrieve relevant movies using RAG
     */
    private List<Movie> retrieveRelevantMovies(String query) {
        log.debug("Retrieving relevant movies for query: {}", query);

        // Extract filters from query
        MovieQueryFilters filters = extractFilters(query);

        // Use vector search for semantic similarity
        List<Movie> movies = new ArrayList<>();

        try {
            var vectorResults = vectorSearchService.searchMoviesBySemanticMeaning(
                    query,
                    MAX_RETRIEVED_MOVIES
            );

            // Convert to Movie entities
            List<Long> movieIds = vectorResults.stream()
                    .map(VectorSimilarityResult::getId)
                    .collect(Collectors.toList());

            movies = movieRepository.findAllById(movieIds);

            // Apply filters
            movies = applyFilters(movies, filters);

        } catch (Exception e) {
            log.error("Error in vector search, falling back to keyword search", e);
            // Fallback: Use title search
            movies = movieRepository.searchByTitle(query, PageRequest.of(0, MAX_RETRIEVED_MOVIES))
                    .getContent();
        }

        log.info("Retrieved {} relevant movies", movies.size());
        return movies;
    }

    /**
     * Extract filters from natural language query
     */
    private MovieQueryFilters extractFilters(String query) {
        MovieQueryFilters filters = new MovieQueryFilters();

        // Extract rating requirement (e.g., "rated 4+", "rating above 4")
        Pattern ratingPattern = Pattern.compile("rat(ed|ing)[\\s:]+([0-9.]+)\\+?");
        Matcher ratingMatcher = ratingPattern.matcher(query.toLowerCase());
        if (ratingMatcher.find()) {
            try {
                filters.minRating = Double.parseDouble(ratingMatcher.group(2));
                log.debug("Extracted min rating: {}", filters.minRating);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Extract genre (simple keyword matching)
        String[] genres = {"war", "drama", "action", "comedy", "thriller", "horror",
                "romance", "sci-fi", "science fiction", "animation"};
        for (String genre : genres) {
            if (query.toLowerCase().contains(genre)) {
                filters.genres.add(genre);
            }
        }

        return filters;
    }

    /**
     * Apply extracted filters to movie list
     */
    private List<Movie> applyFilters(List<Movie> movies, MovieQueryFilters filters) {
        return movies.stream()
                .filter(movie -> {
                    // Filter by rating
                    if (filters.minRating != null) {
                        if (movie.getAvgRating() == null ||
                                movie.getAvgRating() < filters.minRating) {
                            return false;
                        }
                    }

                    // Filter by genre
                    if (!filters.genres.isEmpty()) {
                        Set<String> movieGenres = movie.getGenres().stream()
                                .map(g -> g.getName().toLowerCase())
                                .collect(Collectors.toSet());

                        boolean hasGenre = filters.genres.stream()
                                .anyMatch(filterGenre -> movieGenres.stream()
                                        .anyMatch(mg -> mg.contains(filterGenre)));

                        if (!hasGenre) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Build context for LLM with conversation history and retrieved movies
     */
    private String buildContext(ChatConversation conversation, List<Movie> movies) {
        StringBuilder context = new StringBuilder();

        context.append("=== AVAILABLE MOVIES IN DATABASE ===\n\n");

        for (int i = 0; i < Math.min(movies.size(), 15); i++) {
            Movie movie = movies.get(i);
            context.append(String.format("Movie %d:\n", i + 1));
            context.append(String.format("Title: %s (%d)\n",
                    movie.getTitle(),
                    movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : 0));

            // Overview with rich detail
            if (movie.getOverview() != null && !movie.getOverview().isEmpty()) {
                context.append(String.format("Plot: %s\n", movie.getOverview()));
            }

            // Genres
            if (!movie.getGenres().isEmpty()) {
                String genres = movie.getGenres().stream()
                        .map(g -> g.getName())
                        .collect(Collectors.joining(", "));
                context.append(String.format("Genres: %s\n", genres));
            }

            // Directors
            if (!movie.getDirectors().isEmpty()) {
                String directors = String.join(", ", movie.getDirectors());
                context.append(String.format("Director(s): %s\n", directors));
            }

            // Cast
            if (!movie.getCast().isEmpty()) {
                String cast = movie.getCast().stream()
                        .limit(5)
                        .collect(Collectors.joining(", "));
                context.append(String.format("Cast: %s\n", cast));
            }

            // Keywords for thematic understanding
            if (!movie.getKeywords().isEmpty()) {
                String keywords = movie.getKeywords().stream()
                        .limit(8)
                        .map(k -> k.getName())
                        .collect(Collectors.joining(", "));
                context.append(String.format("Themes/Keywords: %s\n", keywords));
            }

            // Ratings
            if (movie.getAvgRating() != null) {
                context.append(String.format("User Rating: %.1f/5 (%d ratings)\n",
                        movie.getAvgRating(),
                        movie.getRatingCount() != null ? movie.getRatingCount() : 0));
            }

            if (movie.getVoteAverage() != null) {
                context.append(String.format("TMDb Rating: %.1f/10\n", movie.getVoteAverage()));
            }

            context.append("\n");
        }

        return context.toString();
    }

    /**
     * Generate response using LLM with RAG context
     */
    private String generateResponse(ChatConversation conversation,
                                    String userMessage,
                                    String context) {

        // Build conversation history
        List<OllamaLLMService.ChatMessageContext> messages = new ArrayList<>();

        // Get recent messages
        List<ChatMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversation.getId()
        );

        // Take last N messages for context
        int start = Math.max(0, history.size() - MAX_CONTEXT_MESSAGES);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            messages.add(new OllamaLLMService.ChatMessageContext(
                    msg.getRole().name().toLowerCase(),
                    msg.getContent()
            ));
        }

        // Add current user message
        messages.add(new OllamaLLMService.ChatMessageContext("user", userMessage));

        // System prompt with RAG context
        String systemPrompt = buildSystemPrompt(context);

        return llmService.chat(messages, systemPrompt);
    }

    /**
     * Build system prompt with RAG context
     */
    private String buildSystemPrompt(String context) {
        return """
                You are a knowledgeable and passionate movie recommendation assistant with deep understanding of cinema. Your goal is to help users discover movies they'll love by providing thoughtful, detailed recommendations.
                
                CORE RULES:
                1. ONLY discuss movies, films, and cinema-related topics
                2. Use ONLY the movies from the provided database below
                3. Never recommend movies that aren't in the database
                
                RECOMMENDATION STYLE:
                1. **Explain WHY** - Always explain why you're recommending a movie
                   - What themes, tone, or style similarities exist?
                   - What specific elements connect it to the user's request?
                   - What makes it special or worth watching?
                
                2. **Be Conversational** - Write like a knowledgeable friend, not a robot
                   - Use natural language: "I think you'll love...", "If you enjoyed X, then..."
                   - Show enthusiasm when appropriate
                   - Use comparisons to help explain similarities
                
                3. **Provide Context** - Give relevant details:
                   - Mention director if notable
                   - Reference key themes or plot elements (without major spoilers)
                   - Include the rating if it's high (4+ stars)
                   - Mention genre when relevant
                
                4. **Format Naturally**:
                   - Start with a brief introduction addressing their request
                   - Present 3-5 movies with explanations
                   - Use conversational transitions between recommendations
                   - End with an open invitation for follow-up questions
                
                EXAMPLE FORMAT:
                "Since you loved Interstellar's exploration of time and space, I have some great recommendations:
                
                **Arrival** is a perfect match - it shares Interstellar's intellectual approach to sci-fi, dealing with time perception and communication in deeply emotional ways. Like Interstellar, it's more about ideas and human connection than action. Rating: 4.2/5
                
                **The Martian** captures that same sense of wonder about space and human resilience. While it's lighter in tone than Interstellar, it shares the scientific accuracy and the theme of survival against impossible odds. Rating: 4.5/5"
                
                """ + context + """
                
                Now, based on the movies available above, provide thoughtful recommendations that explain WHY each movie matches what the user is looking for. Remember to be specific, insightful, and conversational.
                """;
    }

    /**
     * Extract movie IDs mentioned in assistant response
     */
    private List<Long> extractMovieIds(String response, List<Movie> availableMovies) {
        List<Long> movieIds = new ArrayList<>();

        // Try to match movie titles in the response
        for (Movie movie : availableMovies) {
            if (response.contains(movie.getTitle())) {
                movieIds.add(movie.getId());
            }
        }

        return movieIds;
    }

    /**
     * Get or create user's single conversation
     */
    private ChatConversation getOrCreateUserConversation(User user) {
        Optional<ChatConversation> existing = conversationRepository.findByUserId(user.getId());

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new conversation for this user
        ChatConversation conversation = ChatConversation.builder()
                .user(user)
                .title("Movie Mentor")
                .messageCount(0)
                .build();

        log.info("Created new mentor conversation for user: {}", user.getUsername());
        return conversationRepository.save(conversation);
    }

    /**
     * Save a message
     */
    private ChatMessage saveMessage(ChatConversation conversation,
                                    MessageRole role,
                                    String content,
                                    boolean isOffTopic) {
        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .isOffTopic(isOffTopic)
                .build();

        return messageRepository.save(message);
    }

    /**
     * Build off-topic response
     */
    private ChatMessageDTO buildOffTopicResponse() {
        String response = "I can only assist with movie-related questions like recommendations, " +
                "finding similar films, exploring genres, or discussing cinema. " +
                "What kind of movies are you in the mood for?";

        return ChatMessageDTO.builder()
                .content(response)
                .isOffTopic(true)
                .role(MessageRole.ASSISTANT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Map Movie entity to DTO
     */
    private MovieDTO mapToMovieDTO(Movie movie) {
        return MovieDTO.builder()
                .id(movie.getId())
                .tmdbId(movie.getTmdbId())
                .title(movie.getTitle())
                .overview(movie.getOverview())
                .posterPath(movie.getPosterPath())
                .avgRating(movie.getAvgRating())
                .ratingCount(movie.getRatingCount())
                .build();
    }

    // Helper class for query filters
    private static class MovieQueryFilters {
        Double minRating;
        Set<String> genres = new HashSet<>();
    }
}