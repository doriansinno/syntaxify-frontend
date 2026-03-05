package com.syntaxify.api.dto;

import java.time.Instant;

public record ErrorResponse(
        String message,
        Instant timestamp
) {
}
