package com.example.FitApp.tryon;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "try_on_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TryOnResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long imageId;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private String resultImageUrl;

    @Column(nullable = false)
    private String status;

    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
