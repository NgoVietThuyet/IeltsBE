package com.example.speaking.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BrowserTranscriptFallbackService implements SpeechToTextService {
    @Override
    public String transcribe(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }
        throw new SpeechServiceException(
                "No server STT provider is configured. Use browser speech recognition or enter the transcript manually.");
    }
}
