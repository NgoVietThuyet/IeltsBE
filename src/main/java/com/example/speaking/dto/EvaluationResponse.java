package com.example.speaking.dto;

import java.util.List;

public record EvaluationResponse(double overallBand, CriterionFeedback fluency,
        CriterionFeedback vocabulary, CriterionFeedback grammar,
        CriterionFeedback pronunciation, List<MistakeDto> mistakes,
        String betterAnswer, String assessmentNote) {}
