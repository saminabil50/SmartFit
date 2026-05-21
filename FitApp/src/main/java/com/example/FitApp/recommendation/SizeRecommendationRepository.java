package com.example.FitApp.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SizeRecommendationRepository extends JpaRepository<SizeRecommendation, Long> {
    List<SizeRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SizeRecommendation> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
}
