package com.example.FitApp.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Image> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
}
