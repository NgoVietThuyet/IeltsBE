package com.example.speaking.service;

import com.example.speaking.config.AppProperties;
import com.example.speaking.dto.EvaluationRequest;
import com.example.speaking.dto.EvaluationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiEvaluationService {
    private final AppProperties properties;
    private final ObjectMapper mapper;
    private final RestClient client;

    public AiEvaluationService(AppProperties properties, ObjectMapper mapper, RestClient.Builder builder) {
        this.properties = properties;
        this.mapper = mapper;
        this.client = builder.build();
    }

    public EvaluationResponse evaluate(EvaluationRequest request) {
        String prompt = """
                Evaluate this IELTS Speaking Part %d answer.
                Question: %s
                Candidate transcript: %s
                Pronunciation evidence: estimated (evaluate phrasing, rhythm, flow, and potential linking sounds/word stress implied by the transcript text)

                Return ONLY JSON with this exact shape:
                {"overallBand":6.5,"fluency":{"band":6.5,"feedback":"..."},
                "vocabulary":{"band":6.5,"feedback":"..."},
                "grammar":{"band":6.5,"feedback":"..."},
                "pronunciation":{"band":6.5,"feedback":"..."},
                "mistakes":[{"original":"...","corrected":"...","explanation":"..."}],
                "betterAnswer":"..."}
                Use IELTS bands in 0.5 increments.

                Rules for betterAnswer:
                - Rewrite and improve the candidate's actual answer; preserve their main ideas, facts, viewpoint, and personal context.
                - Target approximately IELTS Band 6.0: clear, natural, reasonably developed, but not unrealistically advanced.
                - Correct the candidate's grammar and word choice and add only small, logical supporting details.
                - Match IELTS Part %d: Part 1 should be concise (about 3-5 sentences), Part 2 should be a developed long turn
                  (about 180-240 words and cover the cue-card points), and Part 3 should be an analytical response
                  (about 6-9 sentences with reasons/examples).
                - Answer the exact question directly. Never replace the topic, person, place, event, or opinion supplied by the candidate.
                - If the transcript contains no usable answer and only repeats the question, create a relevant Band 6.0 sample answer
                  for that exact question and do not introduce a different prompt.
                """
                .formatted(request.part(), request.question(), request.transcript(), request.part());

        if (properties.gemini().apiKey() == null || properties.gemini().apiKey().isBlank()) {
            throw new AiServiceException("GEMINI_API_KEY is not configured", null);
        }
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", properties.gemini().systemPrompt()))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json"));

        try {
            RestClient.RequestBodySpec call = client.post()
                    .uri(properties.gemini().baseUrl() + "/models/" + properties.gemini().model() + ":generateContent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", properties.gemini().apiKey());
            JsonNode response = call.body(body).retrieve().body(JsonNode.class);
            String content = response.path("candidates").path(0).path("content").path("parts").path(0).path("text")
                    .asText();
            if (content.isBlank()) {
                throw new IllegalArgumentException("Gemini returned an empty response");
            }
            return mapper.readValue(extractJson(content), EvaluationResponse.class);
        } catch (Exception exception) {
            throw new AiServiceException("Gemini evaluation is temporarily unavailable: " + exception.getMessage(),
                    exception);
        }
    }

    private String extractJson(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI response did not contain JSON");
        }
        return value.substring(start, end + 1);
    }
}
