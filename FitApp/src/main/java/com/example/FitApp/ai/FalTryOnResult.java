package com.example.FitApp.ai;

import java.util.List;

public record FalTryOnResult(
        String resultImageUrl,
        String status,
        String provider,
        String model,
        Double confidenceScore,
        List<String> warnings,
        List<Object> rawImages
) {
}
