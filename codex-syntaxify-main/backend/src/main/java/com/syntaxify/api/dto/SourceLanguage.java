package com.syntaxify.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceLanguage {
    DE("de"),
    PYTHON("python"),
    JAVASCRIPT("javascript");

    private final String value;

    SourceLanguage(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SourceLanguage from(String value) {
        for (SourceLanguage language : values()) {
            if (language.value.equalsIgnoreCase(value)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unsupported sourceLanguage: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
