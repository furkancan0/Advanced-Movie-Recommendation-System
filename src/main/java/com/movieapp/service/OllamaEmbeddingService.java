package com.movieapp.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OllamaEmbeddingService {

    private final OkHttpClient client;
    private final String baseUrl;
    private final String model;
    private final int embeddingDimension;
    private final Gson gson;

    public OllamaEmbeddingService(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:nomic-embed-text}") String model,
            @Value("${ollama.embedding-dimension:768}") int embeddingDimension) {

        this.baseUrl = baseUrl;
        this.model = model;
        this.embeddingDimension = embeddingDimension;
        this.gson = new Gson();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        log.info("Ollama Embedding Service initialized: url={}, model={}, dimension={}",
                baseUrl, model, embeddingDimension);
    }

    /**
     * Generate embedding for a text using Ollama
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Empty text provided for embedding generation");
            return new float[embeddingDimension];
        }

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("prompt", text);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/embeddings")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Ollama API error: {}", response.code());
                    return new float[embeddingDimension];
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                // Extract embedding array
                if (jsonResponse.has("embedding")) {
                    List<Double> embeddingList = gson.fromJson(
                            jsonResponse.get("embedding"),
                            List.class
                    );

                    float[] embedding = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        embedding[i] = embeddingList.get(i).floatValue();
                    }

                    log.debug("Generated embedding of size: {}", embedding.length);
                    return embedding;
                }

                log.error("No embedding in response");
                return new float[embeddingDimension];
            }

        } catch (IOException e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            return new float[embeddingDimension];
        }
    }

    /**
     * Convert float array to PGvector
     */
    public PGvector toPGVector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return new PGvector(new float[embeddingDimension]);
        }
        return new PGvector(embedding);
    }

    /**
     * Convert PGvector to float array
     */
    public float[] toFloatArray(PGvector vector) {
        if (vector == null) {
            return new float[embeddingDimension];
        }
        return vector.toArray();
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            log.error("Vector dimensions don't match: {} vs {}", vec1.length, vec2.length);
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Test connection to Ollama
     */
    public boolean testConnection() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                boolean isSuccess = response.isSuccessful();
                log.info("Ollama connection test: {}", isSuccess ? "SUCCESS" : "FAILED");
                return isSuccess;
            }
        } catch (Exception e) {
            log.error("Failed to connect to Ollama: {}", e.getMessage());
            return false;
        }
    }
}