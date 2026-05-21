package com.example.FitApp.fitting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FittingResultRepository extends JpaRepository<FittingResult, Long> {
    List<FittingResult> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<FittingResult> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
}
