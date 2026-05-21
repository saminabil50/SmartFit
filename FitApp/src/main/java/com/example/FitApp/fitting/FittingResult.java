package com.example.FitApp.fitting;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fitting_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FittingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long imageId;

    @Column(nullable = false)
    private Long itemId;

    private Long measurementId;
    private Long recommendationId;
    private Long tryonId;
    private String recommendedSize;

    @Column(nullable = false)
    private String fitStatus;

    @Column(nullable = false)
    private String fitLabel;

    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    private String resultImageUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
