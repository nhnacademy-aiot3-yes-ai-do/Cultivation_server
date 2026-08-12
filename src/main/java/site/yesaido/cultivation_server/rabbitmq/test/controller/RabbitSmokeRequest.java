package site.yesaido.cultivation_server.rabbitmq.test.controller;

import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoUpsertEvent;
import site.yesaido.cultivation_server.rabbitmq.event.SensorRange;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

public record RabbitSmokeRequest(
        //@NotNull
        Long cultivationId,

        //@NotBlank
        String location,

        //@NotBlank
        String locationDetail,

        //@NotBlank
        String deviceModel,

        //@NotBlank
        String deviceName,

        //@NotBlank
        String deviceEui,

        //@Valid
        List<RabbitSmokeSensorSettingRequest> sensorSettings
) {

    public ThresholdInfoEvent toThresholdEvent(
            OffsetDateTime occurredAt
    ) {
        List<SensorRange> ranges = sensorSettings.stream()
                .map(setting -> new SensorRange(
                        normalize(setting.sensorType()),
                        setting.unit(),
                        setting.thresholdMin(),
                        setting.thresholdMax()
                ))
                .toList();

        return new ThresholdInfoEvent(
                cultivationId,
                ranges,
                occurredAt
        );
    }

    public List<SensorInfoUpsertEvent> toSensorEvents(
            OffsetDateTime occurredAt
    ) {
        return sensorSettings.stream()
                .map(setting -> new SensorInfoUpsertEvent(
                        cultivationId,
                        location,
                        locationDetail,
                        deviceModel,
                        deviceName,
                        deviceEui,
                        normalize(setting.sensorType()),
                        setting.unit(),
                        occurredAt
                ))
                .toList();
    }

    private String normalize(String sensorType) {
        return sensorType.trim().toUpperCase(Locale.ROOT);
    }
}