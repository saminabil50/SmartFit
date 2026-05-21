package com.example.FitApp.measurement;

import com.example.FitApp.measurement.dto.EstimateMeasurementRequest;
import com.example.FitApp.measurement.dto.MeasurementListResponse;
import com.example.FitApp.measurement.dto.MeasurementResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/measurements")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    @PostMapping("/estimate")
    @ResponseStatus(HttpStatus.CREATED)
    public MeasurementResponse estimate(Authentication authentication,
                                        @RequestBody EstimateMeasurementRequest request) {
        User user = (User) authentication.getPrincipal();
        return measurementService.estimate(user, request);
    }

    @GetMapping
    public MeasurementListResponse getMyMeasurements(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return measurementService.getMyMeasurements(user);
    }

    @GetMapping("/{measurementId}")
    public MeasurementResponse getMeasurement(Authentication authentication,
                                              @PathVariable Long measurementId) {
        User user = (User) authentication.getPrincipal();
        return measurementService.getMeasurement(user, measurementId);
    }

    @DeleteMapping("/{measurementId}")
    public Map<String, String> deleteMeasurement(Authentication authentication,
                                                 @PathVariable Long measurementId) {
        User user = (User) authentication.getPrincipal();
        measurementService.deleteMeasurement(user, measurementId);
        return Map.of("message", "Measurement deleted successfully");
    }
}
