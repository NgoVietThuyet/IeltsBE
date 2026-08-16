package com.example.speaking.service;

import com.example.speaking.dto.EvaluationRequest;
import com.example.speaking.dto.EvaluationResponse;
import com.example.speaking.model.Question;
import com.example.speaking.model.Topic;
import com.example.speaking.repository.QuestionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SpeakingService {
    private final QuestionRepository repository;
    private final AiEvaluationService evaluationService;

    public SpeakingService(QuestionRepository repository, AiEvaluationService evaluationService) {
        this.repository = repository;
        this.evaluationService = evaluationService;
    }

    public List<Topic> topics(int part) {
        if (part < 1 || part > 3) {
            throw new IllegalArgumentException("Part must be between 1 and 3");
        }
        return repository.findTopics(part);
    }

    public List<Question> questions(String topicId, int part) {
        if (part < 1 || part > 3) throw new IllegalArgumentException("Part must be between 1 and 3");
        return repository.findQuestions(topicId, part);
    }

    public EvaluationResponse evaluate(EvaluationRequest request) {
        return evaluationService.evaluate(request);
    }
}
