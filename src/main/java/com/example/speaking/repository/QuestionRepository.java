package com.example.speaking.repository;

import com.example.speaking.model.Question;
import com.example.speaking.model.Topic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionRepository {
    private final ObjectMapper objectMapper;
    private final Map<Integer, List<Topic>> topicsByPart = new LinkedHashMap<>();
    private final Map<String, List<Question>> questionsByTopic = new LinkedHashMap<>();

    public QuestionRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() throws IOException {
        for (int part = 1; part <= 3; part++) {
            loadPart(part);
        }
    }

    public List<Topic> findTopics(int part) {
        return topicsByPart.getOrDefault(part, List.of());
    }

    public List<Question> findQuestions(String topicId, int part) {
        List<Question> questions = questionsByTopic.get(key(topicId, part));
        if (questions == null) {
            throw new ResourceNotFoundException("Topic not found: " + topicId);
        }
        return questions;
    }

    private void loadPart(int part) throws IOException {
        ClassPathResource resource = new ClassPathResource("questions/Part" + part + ".json");
        try (InputStream stream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(stream);
            List<Topic> topics = new ArrayList<>();
            for (JsonNode node : root.path("topics")) {
                String id = node.path("topicId").asText();
                String name = node.path("topic").asText();
                topics.add(new Topic(id, name, part));

                List<Question> questions = new ArrayList<>();
                if (part == 2) {
                    List<String> bullets = new ArrayList<>();
                    node.path("bulletPoints").forEach(item -> bullets.add(item.asText()));
                    questions.add(new Question(node.path("cueCardId").asText(),
                            node.path("cueCard").asText(), bullets,
                            root.path("preparationSeconds").asInt(60),
                            root.path("speakingSeconds").asInt(120)));
                } else {
                    for (JsonNode question : node.path("questions")) {
                        questions.add(new Question(question.path("id").asText(),
                                question.path("text").asText(), List.of(), null, null));
                    }
                }
                questionsByTopic.put(key(id, part), List.copyOf(questions));
            }
            topicsByPart.put(part, List.copyOf(topics));
        }
    }

    private String key(String topicId, int part) {
        return part + ":" + topicId;
    }
}
