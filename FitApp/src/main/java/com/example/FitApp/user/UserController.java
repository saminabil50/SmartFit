package com.example.FitApp.user;

import com.example.FitApp.auth.dto.UserResponse;
import com.example.FitApp.image.ImageService;
import com.example.FitApp.measurement.MeasurementService;
import com.example.FitApp.user.dto.UpdateProfileRequest;
import com.example.FitApp.user.dto.UpdateProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ImageService imageService;
    private final MeasurementService measurementService;

    @GetMapping("/me")
    public UserResponse getProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return userService.getProfile(user);
    }

    @PatchMapping("/me")
    public UpdateProfileResponse updateProfile(Authentication authentication,
                                               @RequestBody UpdateProfileRequest request) {
        User user = (User) authentication.getPrincipal();
        return userService.updateProfile(user, request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> deleteAccount(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        measurementService.deleteAllUserMeasurements(user);
        imageService.deleteAllUserImages(user);
        userService.deleteUser(user);
        return Map.of("message", "Account deleted successfully");
    }
}
