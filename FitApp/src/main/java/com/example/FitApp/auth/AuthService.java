package com.example.FitApp.auth;

import com.example.FitApp.auth.dto.*;
import com.example.FitApp.user.User;
import com.example.FitApp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserResponse register(RegisterRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        if (request.getEmail() == null || request.getEmail().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        if (request.getPassword() == null || request.getPassword().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");

        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .hashedPassword(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    public LoginResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        if (request.getPassword() == null || request.getPassword().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");

        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");

        String token = jwtUtil.generateToken(email);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("bearer")
                .user(toUserResponse(user))
                .build();
    }

    public UserResponse getMe(User user) {
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
