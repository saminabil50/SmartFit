package com.example.FitApp.measurement;

import com.example.FitApp.image.ImageRepository;
import com.example.FitApp.measurement.dto.EstimateMeasurementRequest;
import com.example.FitApp.measurement.dto.MeasurementListResponse;
import com.example.FitApp.measurement.dto.MeasurementResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final ImageRepository imageRepository;

    /**
     * Estimate body measurements from an uploaded image.
     *
     * NOTE: This is a stub implementation. Real ML-based estimation will replace
     * the proportion formula below in a future phase. Confidence score is set low
     * (0.25) to communicate that values are approximate placeholders.
     */
    public MeasurementResponse estimate(User user, EstimateMeasurementRequest request) {
        if (request.getImageId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image_id is required");

        // Validate image belongs to the authenticated user
        imageRepository.findByIdAndUserId(request.getImageId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Image not found or does not belong to you"));

        // Resolve height: request → user profile → default
        int height = resolveHeight(request.getHeightCm(), user);

        Measurement m = Measurement.builder()
                .userId(user.getId())
                .imageId(request.getImageId())
                .heightCmUsed(height)
                .shoulderWidth(round(height * 0.240))
                .chest(round(height * 0.530))
                .waist(round(height * 0.440))
                .hip(round(height * 0.550))
                .inseam(round(height * 0.470))
                .confidenceScore(0.25) // Stub — replace with real model output
                .build();

        Measurement saved = measurementRepository.save(m);
        return toResponse(saved);
    }

    public MeasurementListResponse getMyMeasurements(User user) {
        List<MeasurementResponse> items = measurementRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        return MeasurementListResponse.builder().items(items).build();
    }

    public MeasurementResponse getMeasurement(User user, Long id) {
        Measurement m = measurementRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Measurement not found"));
        return toResponse(m);
    }

    public void deleteMeasurement(User user, Long id) {
        Measurement m = measurementRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Measurement not found"));
        measurementRepository.delete(m);
    }

    @Transactional
    public void deleteAllUserMeasurements(User user) {
        measurementRepository.deleteByUserId(user.getId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int resolveHeight(Integer requestHeight, User user) {
        if (requestHeight != null && requestHeight >= 50 && requestHeight <= 250) return requestHeight;
        if (user.getHeightCm() != null) return user.getHeightCm();
        return 170; // fallback
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private MeasurementResponse toResponse(Measurement m) {
        return MeasurementResponse.builder()
                .id(m.getId())
                .imageId(m.getImageId())
                .heightCmUsed(m.getHeightCmUsed())
                .shoulderWidth(m.getShoulderWidth())
                .chest(m.getChest())
                .waist(m.getWaist())
                .hip(m.getHip())
                .inseam(m.getInseam())
                .confidenceScore(m.getConfidenceScore())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
