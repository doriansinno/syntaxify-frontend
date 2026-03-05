package com.syntaxify.api.dto;

public record TranslateMeta(
        String sourceLanguage,
        String targetLanguage,
        String model,
        long durationMs
) {
}
