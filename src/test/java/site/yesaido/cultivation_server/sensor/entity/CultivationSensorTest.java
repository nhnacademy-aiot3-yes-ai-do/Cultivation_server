package site.yesaido.cultivation_server.sensor.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CultivationSensorTest {

    private static final Instant MEASURED_AT = Instant.parse("2026-09-07T00:00:00Z");

    @Test
    void newSensorStartsOfflineWithoutMeasurements() {
        Instant before = Instant.now();

        CultivationSensor sensor = sensor();

        assertThat(sensor.getMonitoringStartedAt()).isBetween(before, Instant.now());
        assertThat(sensor.getLastMeasuredAt()).isNull();
        assertThat(sensor.getSensorStatus()).isEqualTo(SensorConnectStatus.OFFLINE);
        assertThat(sensor.isDeleted()).isFalse();
    }

    @Test
    void firstMeasurementInitializesLastMeasuredAt() {
        CultivationSensor sensor = sensor();

        sensor.updateLastMeasuredAt(MEASURED_AT);

        assertThat(sensor.getLastMeasuredAt()).isEqualTo(MEASURED_AT);
    }

    @Test
    void newerMeasurementAdvancesLastMeasuredAt() {
        CultivationSensor sensor = sensor();
        sensor.updateLastMeasuredAt(MEASURED_AT);
        Instant newer = MEASURED_AT.plusSeconds(30);

        sensor.updateLastMeasuredAt(newer);

        assertThat(sensor.getLastMeasuredAt()).isEqualTo(newer);
    }

    @ParameterizedTest
    @ValueSource(longs = {-30, 0})
    void olderOrDuplicateMeasurementDoesNotMoveLastMeasuredAt(long offsetSeconds) {
        CultivationSensor sensor = sensor();
        sensor.updateLastMeasuredAt(MEASURED_AT);

        sensor.updateLastMeasuredAt(MEASURED_AT.plusSeconds(offsetSeconds));

        assertThat(sensor.getLastMeasuredAt()).isEqualTo(MEASURED_AT);
    }

    @Test
    void missingMeasurementTimeIsRejectedWithoutChangingState() {
        CultivationSensor sensor = sensor();
        sensor.updateLastMeasuredAt(MEASURED_AT);

        assertThatThrownBy(() -> sensor.updateLastMeasuredAt(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("measuredAt");
        assertThat(sensor.getLastMeasuredAt()).isEqualTo(MEASURED_AT);
    }

    @Test
    void deletedSensorIgnoresNewMeasurements() {
        CultivationSensor sensor = sensor();
        sensor.updateLastMeasuredAt(MEASURED_AT);
        sensor.toDelete();

        sensor.updateLastMeasuredAt(MEASURED_AT.plusSeconds(30));

        assertThat(sensor.getLastMeasuredAt()).isEqualTo(MEASURED_AT);
        assertThat(sensor.isDeleted()).isTrue();
        assertThat(sensor.getSensorStatus()).isEqualTo(SensorConnectStatus.OFFLINE);
    }

    @Test
    void restoringSensorResetsMonitoringAndClearsPreviousMeasurement() {
        CultivationSensor sensor = sensor();
        sensor.updateLastMeasuredAt(MEASURED_AT);
        sensor.toDelete();
        Instant beforeRestore = Instant.now();

        sensor.toRestore("MODEL-B", "Restored sensor", "ROOM-2", "Shelf-2");

        assertThat(sensor.getMonitoringStartedAt()).isBetween(beforeRestore, Instant.now());
        assertThat(sensor.getLastMeasuredAt()).isNull();
        assertThat(sensor.getSensorStatus()).isEqualTo(SensorConnectStatus.OFFLINE);
        assertThat(sensor.isDeleted()).isFalse();
        assertThat(sensor.getDeviceEui()).isEqualTo("EUI-001");
        assertThat(sensor.getCultivationId()).isEqualTo(1L);
        assertThat(sensor.getDeviceModel()).isEqualTo("MODEL-B");
        assertThat(sensor.getDeviceName()).isEqualTo("Restored sensor");
        assertThat(sensor.getLocation()).isEqualTo("ROOM-2");
        assertThat(sensor.getLocationDetail()).isEqualTo("Shelf-2");
    }

    private CultivationSensor sensor() {
        return new CultivationSensor(1L, "EUI-001", "MODEL-A", "Sensor", "ROOM-1", "Shelf-1");
    }
}
