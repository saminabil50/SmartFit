package com.example.FitApp.tryon;

import com.example.FitApp.tryon.dto.TryOnGenerateRequest;
import com.example.FitApp.tryon.dto.TryOnResultListResponse;
import com.example.FitApp.tryon.dto.TryOnResultResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tryon")
@RequiredArgsConstructor
public class TryOnController {

    private final TryOnService tryOnService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public TryOnResultResponse generate(Authentication authentication,
                                        @RequestBody TryOnGenerateRequest request) {
        User user = (User) authentication.getPrincipal();
        return tryOnService.generate(user, request);
    }

    @GetMapping
    public TryOnResultListResponse getMyResults(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return tryOnService.getMyResults(user);
    }

    @GetMapping("/{tryOnId}")
    public TryOnResultResponse getResult(Authentication authentication,
                                         @PathVariable Long tryOnId) {
        User user = (User) authentication.getPrincipal();
        return tryOnService.getResult(user, tryOnId);
    }

    @DeleteMapping("/{tryOnId}")
    public Map<String, String> deleteResult(Authentication authentication,
                                            @PathVariable Long tryOnId) {
        User user = (User) authentication.getPrincipal();
        tryOnService.deleteResult(user, tryOnId);
        return Map.of("message", "Try-on result deleted successfully");
    }
}
