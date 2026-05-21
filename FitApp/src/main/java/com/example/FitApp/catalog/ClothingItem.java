package com.example.FitApp.catalog;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "clothing_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClothingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;   // shirt, pants, dress, jacket, shoes, accessory

    @Column(nullable = false)
    private String gender;     // male, female, unisex

    // Stored as JSON array string e.g. ["XS","S","M","L","XL"]
    @Column(columnDefinition = "TEXT")
    private String availableSizes;

    private String imageUrl;

    // Stored as JSON object string e.g. {"S":{"chest":90,"waist":75}}
    @Column(columnDefinition = "TEXT")
    private String sizeChart;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
