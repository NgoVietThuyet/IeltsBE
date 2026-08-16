package com.example.speaking.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpeakingControllerTests {
    @Autowired MockMvc mvc;

    @Test
    void returnsPartOneTopics() throws Exception {
        mvc.perform(get("/api/speaking/topics").param("part", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].part").value(1));
    }

    @Test
    void returnsPartTwoCueCard() throws Exception {
        mvc.perform(get("/api/speaking/topics/people_inspire/questions").param("part", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].preparationTime").value(60))
                .andExpect(jsonPath("$[0].bulletPoints").isArray());
    }
}
