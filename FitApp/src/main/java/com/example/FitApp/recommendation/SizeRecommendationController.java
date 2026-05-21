package com.example.FitApp.recommendation;

import com.example.FitApp.recommendation.dto.SizeRecommendationListResponse;
import com.example.FitApp.recommendation.dto.SizeRecommendationRequest;
import com.example.FitApp.recommendation.dto.SizeRecommendationResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class SizeRecommendationController {

    private final SizeRecommendationService recommendationService;

    @PostMapping("/size")
    @ResponseStatus(HttpStatus.CREATED)
    public SizeRecommendationResponse recommendSize(Authentication authentication,
                                                    @RequestBody SizeRecommendationRequest request) {
        User user = (User) authentication.getPrincipal();
        return recommendationService.recommend(user, request);
    }

    @GetMapping
    public SizeRecommendationListResponse getMyRecommendations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return recommendationService.getMyRecommendations(user);
    }

    @GetMapping("/{recommendationId}")
    public SizeRecommendationResponse getRecommendation(Authentication authentication,
                                                        @PathVariable Long recommendationId) {
        User user = (User) authentication.getPrincipal();
        return recommendationService.getRecommendation(user, recommendationId);
    }

    @DeleteMapping("/{recommendationId}")
    public Map<String, String> deleteRecommendation(Authentication authentication,
                                                    @PathVariable Long recommendationId) {
        User user = (User) authentication.getPrincipal();
        recommendationService.deleteRecommendation(user, recommendationId);
        return Map.of("message", "Size recommendation deleted successfully");
    }
}
