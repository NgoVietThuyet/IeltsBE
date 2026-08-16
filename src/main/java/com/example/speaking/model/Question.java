package com.example.speaking.model;

import java.util.List;

public record Question(String id, String text, List<String> bulletPoints,
                       Integer preparationTime, Integer answerTime) {}
