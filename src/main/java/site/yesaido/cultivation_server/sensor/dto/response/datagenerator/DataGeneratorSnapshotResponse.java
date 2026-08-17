package site.yesaido.cultivation_server.sensor.dto.response.datagenerator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

// Data Generator가 시작할 때 복구할 센서와 임계값 전체 snapshot을 표현합니다.
public record DataGeneratorSnapshotResponse(
        OffsetDateTime snapshotAt,
        List<DataGeneratorSensorResponse> sensors,
        List<DataGeneratorThresholdResponse> thresholds
) {

    public DataGeneratorSnapshotResponse {
        Objects.requireNonNull(snapshotAt, "snapshotAt은 null일 수 없습니다.");

        sensors = List.copyOf(Objects.requireNonNull(sensors, "sensors는 null일 수 없습니다."));

        thresholds = List.copyOf(Objects.requireNonNull(thresholds, "thresholds는 null일 수 없습니다."));
    }
}
