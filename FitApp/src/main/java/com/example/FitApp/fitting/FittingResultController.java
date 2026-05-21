package com.example.FitApp.fitting;

import com.example.FitApp.fitting.dto.FittingResultListResponse;
import com.example.FitApp.fitting.dto.FittingResultRequest;
import com.example.FitApp.fitting.dto.FittingResultResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fitting-results")
@RequiredArgsConstructor
public class FittingResultController {

    private final FittingResultService fittingResultService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FittingResultResponse create(Authentication authentication,
                                        @RequestBody FittingResultRequest request) {
        User user = (User) authentication.getPrincipal();
        return fittingResultService.create(user, request);
    }

    @GetMapping
    public FittingResultListResponse getMyResults(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return fittingResultService.getMyResults(user);
    }

    @GetMapping("/{resultId}")
    public FittingResultResponse getResult(Authentication authentication,
                                           @PathVariable Long resultId) {
        User user = (User) authentication.getPrincipal();
        return fittingResultService.getResult(user, resultId);
    }

    @DeleteMapping("/{resultId}")
    public Map<String, String> deleteResult(Authentication authentication,
                                            @PathVariable Long resultId) {
        User user = (User) authentication.getPrincipal();
        fittingResultService.deleteResult(user, resultId);
        return Map.of("message", "Fitting result deleted successfully");
    }
}
