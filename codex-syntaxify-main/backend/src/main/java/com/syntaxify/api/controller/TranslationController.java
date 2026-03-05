package com.syntaxify.api.controller;

import com.syntaxify.api.dto.TranslateRequest;
import com.syntaxify.api.dto.TranslateResponse;
import com.syntaxify.api.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TranslationController {

    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping("/translate")
    public TranslateResponse translate(@Valid @RequestBody TranslateRequest request) {
        return translationService.translate(request);
    }
}
