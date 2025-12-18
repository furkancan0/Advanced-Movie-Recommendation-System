package com.movieapp.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OllamaLLMService {

    private final OkHttpClient client;
    private final String baseUrl;
    private final String chatModel;
    private final Gson gson;

    public OllamaLLMService(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.chat-model:llama3.2}") String chatModel) {

        this.baseUrl = baseUrl;
        this.chatModel = chatModel;
        this.gson = new Gson();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        log.info("Ollama LLM Service initialized: url={}, model={}", baseUrl, chatModel);
    }

    /**
     * Chat with Ollama with conversation history
     */
    public String chat(List<ChatMessageContext> messages, String systemPrompt) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", chatModel);
            requestBody.addProperty("stream", false);

            // Build messages array
            JsonArray messagesArray = new JsonArray();

            // Add system prompt
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", systemPrompt);
                messagesArray.add(systemMsg);
            }

            // Add conversation history
            for (ChatMessageContext msg : messages) {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", msg.getRole());
                msgObj.addProperty("content", msg.getContent());
                messagesArray.add(msgObj);
            }

            requestBody.add("messages", messagesArray);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/chat")
                    .post(body)
                    .build();

            log.debug("Sending chat request to Ollama with {} messages", messages.size());

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Ollama chat error: {}", response.code());
                    return "I apologize, but I'm having trouble processing your request right now.";
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.has("message")) {
                    JsonObject messageObj = jsonResponse.getAsJsonObject("message");
                    String content = messageObj.get("content").getAsString();
                    log.debug("Received response from Ollama ({} chars)", content.length());
                    return content;
                }

                log.error("No message in Ollama response");
                return "I apologize, but I couldn't generate a proper response.";
            }

        } catch (IOException e) {
            log.error("Error chatting with Ollama: {}", e.getMessage(), e);
            return "I apologize, but I encountered an error while processing your request.";
        }
    }

    /**
     * Simple single message chat
     */
    public String chat(String userMessage, String systemPrompt) {
        List<ChatMessageContext> messages = new ArrayList<>();
        messages.add(new ChatMessageContext("user", userMessage));
        return chat(messages, systemPrompt);
    }

    /**
     * Test if specific model is available
     */
    public boolean isModelAvailable(String modelName) {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    return responseBody.contains(modelName);
                }
            }
        } catch (Exception e) {
            log.error("Error checking model availability: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Check if chat model is available
     */
    public boolean isChatModelAvailable() {
        return isModelAvailable(chatModel);
    }

    // Inner class for message context
    public static class ChatMessageContext {
        private final String role;
        private final String content;

        public ChatMessageContext(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}