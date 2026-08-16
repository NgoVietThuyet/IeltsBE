package com.example.speaking;

import com.example.speaking.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SpeakingApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpeakingApplication.class, args);
    }
}
