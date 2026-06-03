package com.example.financialassets.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OllamaService {

    private final RestTemplate restTemplate;

    public OllamaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String ask(String prompt) {
        String url = "http://localhost:11434/api/generate";

        Map<String, Object> body = Map.of(
                "model", "llama3.2",
                "prompt", prompt,
                "stream", false
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);

        Object answer = response.getBody().get("response");
        return answer == null ? "" : answer.toString();
    }
}