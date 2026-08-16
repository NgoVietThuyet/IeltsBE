package com.example.speaking.controller;

import com.example.speaking.dto.EvaluationRequest;
import com.example.speaking.dto.EvaluationResponse;
import com.example.speaking.model.Question;
import com.example.speaking.model.Topic;
import com.example.speaking.service.SpeakingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/speaking")
public class SpeakingController {
    private final SpeakingService service;

    public SpeakingController(SpeakingService service) {
        this.service = service;
    }

    @GetMapping("/topics")
    public List<Topic> topics(@RequestParam int part) {
        return service.topics(part);
    }

    @GetMapping("/topics/{topicId}/questions")
    public List<Question> questions(@PathVariable String topicId, @RequestParam int part) {
        return service.questions(topicId, part);
    }

    @PostMapping("/evaluate")
    public EvaluationResponse evaluate(@Valid @RequestBody EvaluationRequest request) {
        return service.evaluate(request);
    }
}
