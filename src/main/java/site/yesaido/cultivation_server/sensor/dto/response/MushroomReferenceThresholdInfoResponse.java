package site.yesaido.cultivation_server.sensor.dto.response;

import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThresholdType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record MushroomReferenceThresholdInfoResponse(
        Long id,
        SensorTypeInfoResponse sensorType,
        MushroomReferenceThresholdType thresholdType,
        BigDecimal thresholdMin,
        BigDecimal thresholdMax,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static List<MushroomReferenceThresholdInfoResponse> from(Set<MushroomReferenceThreshold> thresholds) {
        List<MushroomReferenceThresholdInfoResponse> thresholdInfoResponses = new ArrayList<>();
        for (MushroomReferenceThreshold threshold : thresholds) {
            SensorTypeInfoResponse sensorType = SensorTypeInfoResponse.from(threshold.getSensorType());

            MushroomReferenceThresholdInfoResponse thresholdInfoResponse = new MushroomReferenceThresholdInfoResponse(
                    threshold.getId(), sensorType, threshold.getThresholdType(), threshold.getThresholdMin(), threshold.getThresholdMax(),
                    threshold.getCreatedAt(), threshold.getUpdatedAt());
            thresholdInfoResponses.add(thresholdInfoResponse);
        }
        return thresholdInfoResponses;
    }
}
