package com.example.FitApp.measurement;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "measurements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // Nullable: measurement can be saved without an image in the future
    private Long imageId;

    private Integer heightCmUsed;

    // All values in centimeters
    private Double shoulderWidth;
    private Double chest;
    private Double waist;
    private Double hip;
    private Double inseam;

    // 0.0 = stub/placeholder, 1.0 = high confidence
    @Column(nullable = false)
    private Double confidenceScore;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
