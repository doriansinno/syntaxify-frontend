package com.syntaxify.api.dto;

import java.util.List;

public record TranslateResponse(
        String translatedCode,
        String explanation,
        List<String> warnings,
        TranslateMeta meta
) {
}
