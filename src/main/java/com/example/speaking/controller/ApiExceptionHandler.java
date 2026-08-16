package com.example.speaking.controller;

import com.example.speaking.repository.ResourceNotFoundException;
import com.example.speaking.service.AiServiceException;
import com.example.speaking.service.SpeechServiceException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, Object> notFound(RuntimeException exception) { return error(exception.getMessage()); }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> badRequest(Exception exception) { return error("Invalid request"); }

    @ExceptionHandler(SpeechServiceException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    Map<String, Object> speech(RuntimeException exception) { return error(exception.getMessage()); }

    @ExceptionHandler(AiServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    Map<String, Object> ai(RuntimeException exception) { return error(exception.getMessage()); }

    private Map<String, Object> error(String message) {
        return Map.of("message", message, "timestamp", Instant.now().toString());
    }
}
