package com.example.FitApp.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class AiHealthController {

    private final AiClient aiClient;

    /**
     * GET /api/v1/health/ai
     *
     * Checks whether the Python AI server is reachable.
     * Requires authentication (same as all other endpoints).
     */
    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> checkAiHealth() {
        Map<String, Object> aiHealth = aiClient.checkAiHealth();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ai_server_available", aiHealth != null);
        response.put("ai_server_url", aiClient.getAiServerUrl());
        response.put("ai_health", aiHealth);
        return ResponseEntity.ok(response);
    }
}
