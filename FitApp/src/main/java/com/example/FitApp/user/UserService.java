package com.example.FitApp.user;

import com.example.FitApp.auth.dto.UserResponse;
import com.example.FitApp.user.dto.UpdateProfileRequest;
import com.example.FitApp.user.dto.UpdateProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> ALLOWED_GENDERS =
            Set.of("male", "female", "other", "prefer_not_to_say");
    private static final Set<String> ALLOWED_SIZE_SYSTEMS =
            Set.of("US", "UK", "EU", "INT");

    private final UserRepository userRepository;

    public UserResponse getProfile(User user) {
        return toResponse(user);
    }

    public UpdateProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.getFullName() != null) {
            if (request.getFullName().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name cannot be empty");
            user.setFullName(request.getFullName().trim());
        }
        if (request.getGender() != null) {
            if (!ALLOWED_GENDERS.contains(request.getGender()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Gender must be one of: male, female, other, prefer_not_to_say");
            user.setGender(request.getGender());
        }
        if (request.getHeightCm() != null) {
            if (request.getHeightCm() < 50 || request.getHeightCm() > 250)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Height must be between 50 and 250 cm");
            user.setHeightCm(request.getHeightCm());
        }
        if (request.getWeightKg() != null) {
            if (request.getWeightKg() < 20 || request.getWeightKg() > 300)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weight must be between 20 and 300 kg");
            user.setWeightKg(request.getWeightKg());
        }
        if (request.getPreferredSizeSystem() != null) {
            if (!ALLOWED_SIZE_SYSTEMS.contains(request.getPreferredSizeSystem()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Size system must be one of: US, UK, EU, INT");
            user.setPreferredSizeSystem(request.getPreferredSizeSystem());
        }

        User saved = userRepository.save(user);
        return UpdateProfileResponse.builder()
                .message("Profile updated successfully")
                .profile(toResponse(saved))
                .build();
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .gender(user.getGender())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .preferredSizeSystem(user.getPreferredSizeSystem())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
