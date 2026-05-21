package com.example.FitApp.recommendation;

import com.example.FitApp.catalog.ClothingItem;
import com.example.FitApp.catalog.ClothingItemRepository;
import com.example.FitApp.measurement.Measurement;
import com.example.FitApp.measurement.MeasurementRepository;
import com.example.FitApp.preferences.PreferencesService;
import com.example.FitApp.recommendation.dto.*;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SizeRecommendationService {

    private static final Set<String> ALLOWED_FIT_PREFERENCES = Set.of("tight", "regular", "loose");
    private static final List<String> UPPER_CATEGORIES = List.of("tshirt", "shirt", "top", "tops", "hoodie", "jacket", "sweater", "outerwear");
    private static final List<String> LOWER_CATEGORIES = List.of("pants", "jeans", "shorts", "skirt", "bottom", "bottoms");
    private static final List<String> DRESS_CATEGORIES = List.of("dress", "dresses", "jumpsuit");

    private final SizeRecommendationRepository recommendationRepository;
    private final MeasurementRepository measurementRepository;
    private final ClothingItemRepository clothingItemRepository;
    private final PreferencesService preferencesService;
    private final ObjectMapper objectMapper;

    public SizeRecommendationResponse recommend(User user, SizeRecommendationRequest request) {
        if (request == null || request.getMeasurementId() == null || request.getItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "measurement_id and item_id are required");
        }

        String requestedFitPreference = request.getFitPreference();
        String fitPreference = normalizeFitPreference(
                requestedFitPreference == null || requestedFitPreference.isBlank()
                        ? preferencesService.preferredFitOrDefault(user)
                        : requestedFitPreference);
        Measurement measurement = measurementRepository.findByIdAndUserId(request.getMeasurementId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Measurement not found"));
        ClothingItem item = clothingItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));

        List<SizeRow> sizeRows = parseSizeChart(item.getSizeChart());
        if (sizeRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected clothing item does not have a size chart");
        }

        List<String> fields = relevantFields(item.getCategory(), sizeRows);
        if (fields.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected clothing item does not have a size chart");
        }

        List<ScoredSize> scored = sizeRows.stream()
                .map(row -> scoreSize(row, measurement, fields))
                .sorted((a, b) -> {
                    int scoreCmp = Double.compare(b.score, a.score);
                    if (scoreCmp != 0) return scoreCmp;
                    return Integer.compare(preferenceRank(a.index, fitPreference), preferenceRank(b.index, fitPreference));
                })
                .toList();

        ScoredSize best = applyFitPreference(scored, fitPreference);
        List<RecommendationAlternativeResponse> alternatives = scored.stream()
                .filter(score -> !score.size.equals(best.size))
                .limit(2)
                .map(score -> RecommendationAlternativeResponse.builder()
                        .size(score.size)
                        .fitType(classifyFit(score.index, best.index))
                        .confidenceScore(round(score.score))
                        .reason(alternativeReason(score.size, classifyFit(score.index, best.index)))
                        .build())
                .toList();

        String reason = buildReason(best, fields);
        SizeRecommendation recommendation = SizeRecommendation.builder()
                .userId(user.getId())
                .measurementId(measurement.getId())
                .itemId(item.getId())
                .recommendedSize(best.size)
                .fitPreference(fitPreference)
                .fitType(best.fitType)
                .confidenceScore(round(best.score))
                .reason(reason)
                .alternatives(serializeAlternatives(alternatives))
                .build();

        return toResponse(recommendationRepository.save(recommendation), item);
    }

    public SizeRecommendationListResponse getMyRecommendations(User user) {
        List<SizeRecommendationResponse> items = recommendationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        return SizeRecommendationListResponse.builder().items(items).build();
    }

    public SizeRecommendationResponse getRecommendation(User user, Long recommendationId) {
        SizeRecommendation recommendation = recommendationRepository.findByIdAndUserId(recommendationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Size recommendation not found"));
        return toResponse(recommendation);
    }

    @Transactional
    public void deleteRecommendation(User user, Long recommendationId) {
        SizeRecommendation recommendation = recommendationRepository.findByIdAndUserId(recommendationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Size recommendation not found"));
        recommendationRepository.delete(recommendation);
    }

    private String normalizeFitPreference(String fitPreference) {
        String value = (fitPreference == null || fitPreference.isBlank()) ? "regular" : fitPreference.trim().toLowerCase();
        if (!ALLOWED_FIT_PREFERENCES.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid fit_preference. Allowed values: tight, regular, loose");
        }
        return value;
    }

    private List<SizeRow> parseSizeChart(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            Object raw = objectMapper.readValue(json, Object.class);
            if (raw instanceof List<?> list) return parseListChart(list);
            if (raw instanceof Map<?, ?> map) return parseMapChart(map);
        } catch (JacksonException ignored) {
            return List.of();
        }
        return List.of();
    }

    private List<SizeRow> parseListChart(List<?> list) {
        List<SizeRow> rows = new ArrayList<>();
        int index = 0;
        for (Object entry : list) {
            if (entry instanceof Map<?, ?> map) {
                String size = stringValue(map.get("size"));
                if (size != null && !size.isBlank()) rows.add(new SizeRow(size, map, index++));
            }
        }
        return rows;
    }

    private List<SizeRow> parseMapChart(Map<?, ?> map) {
        Object sizes = map.get("sizes");
        if (sizes instanceof List<?> list) return parseListChart(list);

        List<SizeRow> rows = new ArrayList<>();
        int index = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> measurements) {
                rows.add(new SizeRow(String.valueOf(entry.getKey()), measurements, index++));
            }
        }
        return rows;
    }

    private List<String> relevantFields(String category, List<SizeRow> rows) {
        List<String> preferred;
        String normalizedCategory = category == null ? "" : category.toLowerCase();
        if (UPPER_CATEGORIES.contains(normalizedCategory)) {
            preferred = List.of("chest", "shoulderWidth", "waist");
        } else if (LOWER_CATEGORIES.contains(normalizedCategory)) {
            preferred = List.of("waist", "hip", "inseam");
        } else if (DRESS_CATEGORIES.contains(normalizedCategory)) {
            preferred = List.of("chest", "waist", "hip", "shoulderWidth");
        } else {
            preferred = List.of("chest", "waist", "hip", "shoulderWidth", "inseam");
        }

        return preferred.stream()
                .filter(field -> rows.stream().anyMatch(row -> row.rangeFor(field).isPresent()))
                .toList();
    }

    private ScoredSize scoreSize(SizeRow row, Measurement measurement, List<String> fields) {
        double total = 0.0;
        int count = 0;
        List<String> matched = new ArrayList<>();
        for (String field : fields) {
            Optional<Range> range = row.rangeFor(field);
            Double value = measurementValue(measurement, field);
            if (range.isEmpty() || value == null) continue;
            total += scoreMeasurement(value, range.get(), toleranceFor(field));
            count++;
            matched.add(labelFor(field));
        }

        double score = count == 0 ? 0.0 : total / count;
        String fitType = fitTypeFor(row, measurement, fields);
        return new ScoredSize(row.size, row.index, score, fitType, matched);
    }

    private double scoreMeasurement(double value, Range range, double tolerance) {
        if (value >= range.min && value <= range.max) {
            double center = (range.min + range.max) / 2.0;
            double halfWidth = Math.max((range.max - range.min) / 2.0, 1.0);
            return Math.max(0.85, 1.0 - (Math.abs(value - center) / halfWidth) * 0.15);
        }

        double diff = value < range.min ? range.min - value : value - range.max;
        if (diff <= tolerance) {
            return Math.max(0.55, 0.85 - (diff / tolerance) * 0.30);
        }
        return Math.max(0.0, 0.55 - ((diff - tolerance) / (tolerance * 3.0)) * 0.55);
    }

    private ScoredSize applyFitPreference(List<ScoredSize> scored, String fitPreference) {
        ScoredSize best = scored.getFirst();
        for (ScoredSize candidate : scored) {
            if (best.score - candidate.score > 0.05) break;
            if ("tight".equals(fitPreference) && candidate.index < best.index) best = candidate;
            if ("loose".equals(fitPreference) && candidate.index > best.index) best = candidate;
        }
        return best;
    }

    private int preferenceRank(int index, String fitPreference) {
        if ("tight".equals(fitPreference)) return index;
        if ("loose".equals(fitPreference)) return -index;
        return 0;
    }

    private String fitTypeFor(SizeRow row, Measurement measurement, List<String> fields) {
        double centerDiff = 0.0;
        int count = 0;
        for (String field : fields) {
            Optional<Range> range = row.rangeFor(field);
            Double value = measurementValue(measurement, field);
            if (range.isEmpty() || value == null) continue;
            centerDiff += value - ((range.get().min + range.get().max) / 2.0);
            count++;
        }
        if (count == 0) return "regular";
        double avg = centerDiff / count;
        if (avg > 2.0) return "tight";
        if (avg < -2.0) return "loose";
        return "regular";
    }

    private String classifyFit(int candidateIndex, int bestIndex) {
        if (candidateIndex < bestIndex) return "tight";
        if (candidateIndex > bestIndex) return "loose";
        return "regular";
    }

    private String buildReason(ScoredSize best, List<String> fields) {
        String joinedFields = best.matchedFields.isEmpty()
                ? String.join(" and ", fields.stream().map(this::labelFor).toList())
                : String.join(" and ", best.matchedFields);
        if (best.score >= 0.85) {
            return capitalize(joinedFields) + " measurements fit best within size " + best.size + ".";
        }
        return "Size " + best.size + " is the closest match based on " + joinedFields + " measurements.";
    }

    private String alternativeReason(String size, String fitType) {
        return size + " may provide a " + fitType + " fit.";
    }

    private SizeRecommendationResponse toResponse(SizeRecommendation recommendation) {
        ClothingItem item = clothingItemRepository.findById(recommendation.getItemId()).orElse(null);
        return toResponse(recommendation, item);
    }

    private SizeRecommendationResponse toResponse(SizeRecommendation recommendation, ClothingItem item) {
        return SizeRecommendationResponse.builder()
                .id(recommendation.getId())
                .measurementId(recommendation.getMeasurementId())
                .itemId(recommendation.getItemId())
                .recommendedSize(recommendation.getRecommendedSize())
                .fitPreference(recommendation.getFitPreference())
                .fitType(recommendation.getFitType())
                .confidenceScore(recommendation.getConfidenceScore())
                .reason(recommendation.getReason())
                .alternatives(deserializeAlternatives(recommendation.getAlternatives()))
                .createdAt(recommendation.getCreatedAt())
                .clothingItem(toClothingItemResponse(item))
                .build();
    }

    private RecommendationClothingItemResponse toClothingItemResponse(ClothingItem item) {
        if (item == null) return null;
        return RecommendationClothingItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .imageUrl(item.getImageUrl())
                .build();
    }

    private String serializeAlternatives(List<RecommendationAlternativeResponse> alternatives) {
        try {
            return objectMapper.writeValueAsString(alternatives);
        } catch (JacksonException e) {
            return null;
        }
    }

    private List<RecommendationAlternativeResponse> deserializeAlternatives(String alternatives) {
        if (alternatives == null || alternatives.isBlank()) return List.of();
        try {
            return objectMapper.readValue(alternatives, new TypeReference<>() {});
        } catch (JacksonException e) {
            return List.of();
        }
    }

    private Double measurementValue(Measurement measurement, String field) {
        return switch (field) {
            case "chest" -> measurement.getChest();
            case "waist" -> measurement.getWaist();
            case "hip" -> measurement.getHip();
            case "shoulderWidth" -> measurement.getShoulderWidth();
            case "inseam" -> measurement.getInseam();
            default -> null;
        };
    }

    private double toleranceFor(String field) {
        return switch (field) {
            case "shoulderWidth" -> 2.0;
            case "inseam" -> 3.0;
            default -> 4.0;
        };
    }

    private String labelFor(String field) {
        return switch (field) {
            case "shoulderWidth" -> "shoulder";
            default -> field;
        };
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double numberValue(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private double round(double value) {
        return Math.round(Math.max(0.0, Math.min(1.0, value)) * 100.0) / 100.0;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private record Range(double min, double max) {}

    private record SizeRow(String size, Map<?, ?> data, int index) {
        Optional<Range> rangeFor(String field) {
            List<String> bases = switch (field) {
                case "shoulderWidth" -> List.of("shoulder_width", "shoulderWidth", "shoulder");
                default -> List.of(field);
            };

            for (String base : bases) {
                Double min = number(data, base + "_cm_min", base + "_min", base + "CmMin");
                Double max = number(data, base + "_cm_max", base + "_max", base + "CmMax");
                if (min != null && max != null) return Optional.of(new Range(Math.min(min, max), Math.max(min, max)));

                Double exact = number(data, base + "_cm", base + "Cm", base);
                if (exact != null) {
                    double padding = "shoulderWidth".equals(field) ? 2.0 : "inseam".equals(field) ? 3.0 : 4.0;
                    return Optional.of(new Range(exact - padding, exact + padding));
                }
            }
            return Optional.empty();
        }

        private static Double number(Map<?, ?> data, String... keys) {
            for (String key : keys) {
                if (data.containsKey(key)) {
                    Object value = data.get(key);
                    if (value instanceof Number number) return number.doubleValue();
                    if (value instanceof String string) {
                        try {
                            return Double.parseDouble(string);
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                    }
                }
            }
            return null;
        }
    }

    private record ScoredSize(String size, int index, double score, String fitType, List<String> matchedFields) {}
}
