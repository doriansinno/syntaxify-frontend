package com.syntaxify.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syntaxify.api.dto.TargetLanguage;
import com.syntaxify.api.dto.TranslateMeta;
import com.syntaxify.api.dto.TranslateRequest;
import com.syntaxify.api.dto.TranslateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TranslationService {

    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
            "class", "extends", "implements", "inheritance", "oop", "rekursion", "recursion",
            "thread", "multithreading", "async", "await", "sql", "database", "jdbc", "file", "filesystem",
            "fetch(", "axios", "httpclient", "spring", "react", "vue", "angular", "django", "flask", "api"
    );

    private static final String SCOPE_WARNING = "Diese Version von Syntaxify unterstützt nur einfache Grundlagen wie Variablen, Funktionen, Schleifen und Bedingungen.";

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TranslationService(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model,
            @Value("${openai.timeout-ms}") int timeoutMs,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public TranslateResponse translate(TranslateRequest request) {
        long start = System.currentTimeMillis();
        List<String> warnings = detectOutOfScopeWarnings(request.input());

        if (!warnings.isEmpty()) {
            return new TranslateResponse(
                    "",
                    "Anfrage außerhalb des unterstützten Umfangs erkannt.",
                    warnings,
                    new TranslateMeta(request.sourceLanguage().getValue(), request.targetLanguage().getValue(), "scope-guard", System.currentTimeMillis() - start)
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_API_KEY is not configured");
        }

        try {
            String responseContent = callOpenAi(request);
            Map<String, Object> parsed = objectMapper.readValue(responseContent, new TypeReference<>() {
            });

            String translatedCode = (String) parsed.getOrDefault("translatedCode", "");
            String explanation = (String) parsed.getOrDefault("explanation", "");
            List<String> modelWarnings = objectMapper.convertValue(parsed.getOrDefault("warnings", List.of()), new TypeReference<>() {
            });

            List<String> mergedWarnings = new ArrayList<>(modelWarnings);
            long duration = System.currentTimeMillis() - start;

            return new TranslateResponse(
                    translatedCode,
                    explanation,
                    mergedWarnings,
                    new TranslateMeta(request.sourceLanguage().getValue(), request.targetLanguage().getValue(), model, duration)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI request interrupted", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI request failed", e);
        }
    }

    private List<String> detectOutOfScopeWarnings(String input) {
        String normalized = input.toLowerCase();
        List<String> warnings = new ArrayList<>();

        for (String keyword : BLOCKED_KEYWORDS) {
            if (normalized.contains(keyword)) {
                warnings.add(SCOPE_WARNING);
                break;
            }
        }

        return warnings;
    }

    private String callOpenAi(TranslateRequest request) throws IOException, InterruptedException {
        String systemPrompt = buildSystemPrompt(request.targetLanguage());
        String userPrompt = "SOURCE_LANGUAGE=" + request.sourceLanguage().getValue()
                + "\nTARGET_LANGUAGE=" + request.targetLanguage().getValue()
                + "\nINPUT:\n" + request.input();

        Map<String, Object> payload = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.1
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI error: " + response.body());
        }

        JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.path("choices").path(0).path("message").path("content").asText();
    }

    private String buildSystemPrompt(TargetLanguage targetLanguage) {
        return """
                You are Syntaxify, a deterministic translator for basic programming concepts.
                You must stay within beginner scope only:
                - variables, primitive data types, arrays/lists, functions, parameters
                - if/else, for loops, while loops
                - print/console.log, arithmetic operations, simple comparisons

                Strictly forbidden topics:
                - classes, OOP, inheritance, recursion
                - async programming, threads, file access, databases, API calls
                - frameworks and advanced libraries

                Output rules:
                1) Return valid JSON only (no markdown fences) with keys:
                   translatedCode (string), explanation (string), warnings (array of strings)
                2) translatedCode must be plain %s code and runnable for simple examples.
                3) explanation must be short (max 3 sentences).
                4) If request is out-of-scope, set translatedCode to empty string and include the warning:
                   \"%s\"
                5) Never output unsupported constructs.
                """.formatted(targetLanguage.getValue(), SCOPE_WARNING);
    }
}
