package com.example.FitApp.preferences;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_preferences_user_id", columnNames = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String preferredSizeSystem;

    @Column(nullable = false)
    private String preferredFit;

    @Column(nullable = false)
    private String preferredGenderCategory;

    @Column(columnDefinition = "TEXT")
    private String preferredCategories;

    @Column(nullable = false)
    private String defaultImageType;

    @Column(nullable = false)
    private Boolean cameraEnabled;

    @Column(nullable = false)
    private Boolean saveUploadedImages;

    @Column(nullable = false)
    private Boolean saveMeasurementHistory;

    @Column(nullable = false)
    private Boolean saveTryonHistory;

    @Column(nullable = false)
    private Boolean dataUsageConsent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
