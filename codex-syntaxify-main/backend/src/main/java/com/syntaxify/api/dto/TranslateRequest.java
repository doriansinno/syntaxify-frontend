package com.syntaxify.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TranslateRequest(
        @NotBlank(message = "input is required")
        String input,
        @NotNull(message = "sourceLanguage is required")
        SourceLanguage sourceLanguage,
        @NotNull(message = "targetLanguage is required")
        TargetLanguage targetLanguage
) {
}
