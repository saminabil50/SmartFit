package com.example.FitApp.tryon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TryOnResultRepository extends JpaRepository<TryOnResult, Long> {
    List<TryOnResult> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<TryOnResult> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
}
