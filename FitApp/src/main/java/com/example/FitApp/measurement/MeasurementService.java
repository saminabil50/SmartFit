package com.example.FitApp.measurement;

import com.example.FitApp.ai.AiClient;
import com.example.FitApp.image.Image;
import com.example.FitApp.image.ImageRepository;
import com.example.FitApp.measurement.dto.EstimateMeasurementRequest;
import com.example.FitApp.measurement.dto.MeasurementListResponse;
import com.example.FitApp.measurement.dto.MeasurementResponse;
import com.example.FitApp.preferences.PreferencesService;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final ImageRepository imageRepository;
    private final PreferencesService preferencesService;
    private final AiClient aiClient;

    public MeasurementResponse estimate(User user, EstimateMeasurementRequest request) {
        if (request.getImageId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image_id is required");

        Image image = imageRepository.findByIdAndUserId(request.getImageId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Image not found or does not belong to you"));

        int height = resolveHeight(request.getHeightCm(), user);
        Measurement m = estimateFromAiOrFallback(image, height, user.getId());

        if (!preferencesService.shouldSaveMeasurementHistory(user)) {
            return toResponse(m);
        }

        return toResponse(measurementRepository.save(m));
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

    @SuppressWarnings("unchecked")
    private Measurement estimateFromAiOrFallback(Image image, int height, Long userId) {
        try {
            Map<String, Object> aiResult = aiClient.estimateMeasurements(image.getFilePath(), (double) height);
            if (aiResult != null) {
                Map<String, Object> measures = (Map<String, Object>) aiResult.get("measurements");
                Number confidence = (Number) aiResult.get("confidence_score");
                if (measures != null && confidence != null) {
                    return Measurement.builder()
                            .userId(userId)
                            .imageId(image.getId())
                            .heightCmUsed(height)
                            .shoulderWidth(doubleFromMap(measures, "shoulder_width_cm"))
                            .chest(doubleFromMap(measures, "chest_cm"))
                            .waist(doubleFromMap(measures, "waist_cm"))
                            .hip(doubleFromMap(measures, "hip_cm"))
                            .inseam(doubleFromMap(measures, "inseam_cm"))
                            .confidenceScore(confidence.doubleValue())
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("AI measurement estimation failed for image {}, using proportion fallback: {}",
                    image.getId(), e.getMessage());
        }
        return proportionFallback(userId, image.getId(), height);
    }

    private Measurement proportionFallback(Long userId, Long imageId, int height) {
        return Measurement.builder()
                .userId(userId)
                .imageId(imageId)
                .heightCmUsed(height)
                .shoulderWidth(round(height * 0.240))
                .chest(round(height * 0.530))
                .waist(round(height * 0.440))
                .hip(round(height * 0.550))
                .inseam(round(height * 0.470))
                .confidenceScore(0.25)
                .build();
    }

    private Double doubleFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return round(n.doubleValue());
        return null;
    }

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
