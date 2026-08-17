package com.example.speaking.service;

import com.example.speaking.config.AppProperties;
import com.example.speaking.dto.EvaluationRequest;
import com.example.speaking.dto.EvaluationResponse;
import com.example.speaking.dto.CriterionFeedback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.ArrayList;
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
                Pronunciation evidence: %s

                Return ONLY JSON with this exact shape:
                {"overallBand":6.0,"fluency":{"band":6.0,"feedback":"..."},
                "vocabulary":{"band":6.0,"feedback":"..."},
                "grammar":{"band":6.0,"feedback":"..."},
                "pronunciation":{"band":null,"feedback":"..."},
                "mistakes":[{"original":"...","corrected":"...","explanation":"..."}],
                "betterAnswer":"...","assessmentNote":"Text-based estimate only."}

                Apply the official IELTS Speaking public band descriptors. Assign WHOLE bands (0-9) to each criterion;
                the backend, not you, calculates the final half-band estimate.

                Fluency and Coherence (transcript evidence only):
                - Band 4: noticeable breakdown or short basic utterances; ideas cannot be developed coherently.
                - Band 5: usually maintains speech but uses repetition/self-correction or slow speech; basic cohesive devices may be overused.
                - Band 6: willing and able to produce longer turns; some hesitation/repetition; generally coherent with adequate topic development.
                - Band 7: readily produces long turns; flexible discourse markers; coherent, relevant development despite some hesitation.
                - Band 8: fluent with only occasional language-related hesitation; coherent, appropriate and relevant development.
                Since audio timing is absent, judge only continuity implied by answer length, coherence, relevance, and development; state this limitation.

                Lexical Resource:
                - Band 4: sufficient only for familiar topics; frequent inappropriate choices; rare paraphrase.
                - Band 5: manages familiar/unfamiliar topics with limited flexibility; attempts paraphrase with mixed success.
                - Band 6: vocabulary wide enough to discuss at length; meaning is clear despite inappropriate choices; generally paraphrases successfully.
                - Band 7: flexible resource across topics; some less common/idiomatic language, style/collocation awareness; effective paraphrase.
                - Band 8: wide, flexible and precise resource; skilful less common/idiomatic usage; effective paraphrase.

                Grammatical Range and Accuracy:
                - Band 4: basic forms and short utterances; subordinate structures are rare; frequent errors may cause misunderstanding.
                - Band 5: basic sentence forms reasonably controlled; limited complex structures with frequent errors that may require reformulation.
                - Band 6: mix of simple and complex structures with limited flexibility; complex errors rarely impede communication.
                - Band 7: range of structures used flexibly; frequent error-free sentences; simple and complex sentences are effective despite some errors.
                - Band 8: wide range used flexibly; majority of sentences error-free; only occasional non-systematic errors.

                Pronunciation:
                - If audio is unavailable, return band null and state that pronunciation cannot be assessed from transcript.
                - If audio is available, assess intelligibility, listener effort, sound accuracy, word stress, sentence stress,
                  rhythm, intonation, and connected speech using the official IELTS public descriptors.
                - Band 4: limited phonological control; frequent mispronunciation; listener strain; some unintelligible speech.
                - Band 5: generally intelligible but limited range/control; mispronunciation causes some listener strain.
                - Band 6: generally understandable throughout; some effective features; occasional mispronunciation reduces clarity.
                - Band 7: all Band 6 strengths plus some Band 8 features; generally easy to understand.
                - Band 8: wide phonological range; sustained rhythm/stress/intonation; easily understood with minimal accent effect.

                Strict scoring rules:
                - Score only language actually demonstrated in the transcript. Never infer skills that are not evidenced.
                - A grammatically correct but very short answer is NOT evidence of Band 6+ fluency, vocabulary range, or grammar range.
                - Part 1 under 8 words or only one undeveloped clause: maximum overall Band 4.0.
                - Part 2 under 60 words: maximum overall Band 4.0.
                - Part 3 under 30 words without a reason/example: maximum overall Band 4.0.
                - An irrelevant answer or one that only repeats the question: maximum overall Band 2.5.
                - No errors in a tiny sample does not justify a high score. Limited evidence must lower the score.
                - Each criterion feedback must refer to exact words in the transcript and give one concrete improvement.
                - Avoid generic IELTS descriptors. Keep every feedback concise, candid, and specific.

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
                .formatted(request.part(), request.question(), request.transcript(),
                        hasUsableAudio(request) ? "audio recording attached; assess it directly" : "unavailable", request.part());

        if (properties.gemini().apiKey() == null || properties.gemini().apiKey().isBlank()) {
            throw new AiServiceException("GEMINI_API_KEY is not configured", null);
        }
        List<Map<String, Object>> contentParts = new ArrayList<>();
        contentParts.add(Map.of("text", prompt));
        if (hasUsableAudio(request)) {
            contentParts.add(Map.of("inlineData", Map.of(
                    "mimeType", request.audioMimeType(),
                    "data", request.audioBase64())));
        }
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", properties.gemini().systemPrompt()))),
                "contents", List.of(Map.of("role", "user", "parts", contentParts)),
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
            EvaluationResponse evaluation = mapper.readValue(extractJson(content), EvaluationResponse.class);
            return calculateScore(applyEvidenceCaps(evaluation, request));
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

    private EvaluationResponse applyEvidenceCaps(EvaluationResponse response, EvaluationRequest request) {
        int wordCount = request.transcript().trim().split("\\s+").length;
        Double cap = switch (request.part()) {
            case 1 -> wordCount < 8 ? 4.0 : null;
            case 2 -> wordCount < 60 ? 4.0 : null;
            case 3 -> wordCount < 30 ? 4.0 : null;
            default -> null;
        };
        if (cap == null) return response;

        return new EvaluationResponse(
                Math.min(response.overallBand(), cap),
                capCriterion(response.fluency(), cap),
                capCriterion(response.vocabulary(), cap),
                capCriterion(response.grammar(), cap),
                response.pronunciation(),
                response.mistakes(),
                response.betterAnswer(),
                response.assessmentNote());
    }

    private CriterionFeedback capCriterion(CriterionFeedback criterion, double cap) {
        if (criterion == null || criterion.band() == null || criterion.band() <= cap) return criterion;
        return new CriterionFeedback(cap, criterion.feedback());
    }

    private EvaluationResponse calculateScore(EvaluationResponse response) {
        CriterionFeedback fluencyFeedback = normalizeCriterion(response.fluency(), "fluency");
        CriterionFeedback vocabularyFeedback = normalizeCriterion(response.vocabulary(), "vocabulary");
        CriterionFeedback grammarFeedback = normalizeCriterion(response.grammar(), "grammar");
        CriterionFeedback pronunciationFeedback = response.pronunciation();
        double fluency = fluencyFeedback.band();
        double vocabulary = vocabularyFeedback.band();
        double grammar = grammarFeedback.band();
        Double pronunciation = pronunciationFeedback == null ? null : pronunciationFeedback.band();
        double average = pronunciation == null
                ? (fluency + vocabulary + grammar) / 3.0
                : (fluency + vocabulary + grammar + pronunciation) / 4.0;
        double overall = Math.round(average * 2.0) / 2.0;
        String note = pronunciation == null
                ? "Provisional text-based estimate from Fluency & Coherence, Lexical Resource, and Grammar only. Pronunciation and audio fluency were not assessed; this is not an official IELTS Speaking score."
                : "Estimated from all four equally weighted IELTS Speaking criteria; this is not an official IELTS score.";
        return new EvaluationResponse(overall, fluencyFeedback, vocabularyFeedback, grammarFeedback,
                pronunciationFeedback, response.mistakes(), response.betterAnswer(), note);
    }

    private boolean hasUsableAudio(EvaluationRequest request) {
        return Boolean.TRUE.equals(request.hasAudio())
                && request.audioBase64() != null && !request.audioBase64().isBlank()
                && request.audioMimeType() != null && !request.audioMimeType().isBlank();
    }

    private CriterionFeedback normalizeCriterion(CriterionFeedback criterion, String name) {
        if (criterion == null || criterion.band() == null) {
            throw new IllegalArgumentException("Gemini omitted the " + name + " band");
        }
        double band = Math.max(0.0, Math.min(9.0, Math.round(criterion.band())));
        return new CriterionFeedback(band, criterion.feedback());
    }
}
