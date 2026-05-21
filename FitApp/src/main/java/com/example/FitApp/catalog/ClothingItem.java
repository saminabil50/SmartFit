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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category;   // shirt, pants, dress, jacket, shoes, accessory

    @Column(nullable = false)
    private String gender;     // male, female, unisex

    private String brand;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'INT'")
    private String sizeSystem;

    // Stored as JSON array string e.g. ["XS","S","M","L","XL"]
    @Column(columnDefinition = "TEXT")
    private String availableSizes;

    private Double basePrice;

    private String currency;

    private String imageUrl;

    // Stored as JSON object string e.g. {"S":{"chest":90,"waist":75}}
    @Column(columnDefinition = "TEXT")
    private String sizeChart;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    void setDefaults() {
        if (sizeSystem == null || sizeSystem.isBlank()) sizeSystem = "INT";
        if (isActive == null) isActive = true;
    }
}
