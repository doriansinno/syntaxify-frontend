package com.syntaxify.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TargetLanguage {
    PYTHON("python"),
    JAVASCRIPT("javascript");

    private final String value;

    TargetLanguage(String value) {
        this.value = value;
    }

    @JsonCreator
    public static TargetLanguage from(String value) {
        for (TargetLanguage language : values()) {
            if (language.value.equalsIgnoreCase(value)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unsupported targetLanguage: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
