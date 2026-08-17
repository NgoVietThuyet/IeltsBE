package com.example.speaking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluationRequest(
        @NotBlank String questionId,
        @NotBlank @Size(max = 2000) String question,
        @NotBlank @Size(max = 10000) String transcript,
        @Min(1) @Max(3) int part,
        Boolean hasAudio,
        @Size(max = 20000000) String audioBase64,
        @Size(max = 100) String audioMimeType
) {}
