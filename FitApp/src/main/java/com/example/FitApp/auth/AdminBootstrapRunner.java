package com.example.FitApp.auth;

import com.example.FitApp.user.User;
import com.example.FitApp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == null || user.getRole().isBlank())
                .forEach(user -> {
                    user.setRole("user");
                    userRepository.save(user);
                });

        if (adminEmail == null || adminEmail.isBlank()) return;
        userRepository.findByEmail(adminEmail.trim().toLowerCase()).ifPresent(user -> {
            if (!"admin".equals(user.getRole())) {
                user.setRole("admin");
                userRepository.save(user);
            }
        });
    }
}
