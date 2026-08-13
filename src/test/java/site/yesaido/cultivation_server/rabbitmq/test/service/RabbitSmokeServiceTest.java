package site.yesaido.cultivation_server.rabbitmq.test.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoUpsertEvent;
import site.yesaido.cultivation_server.rabbitmq.event.SensorRange;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitSmokeServiceTest {

    @Mock
    ApplicationEventPublisher publisher;

    @InjectMocks
    RabbitSmokeService service;

    @Test
    void publishesThresholdAndSensorsWithSameOccurredAt() {
        OffsetDateTime occurredAt =
                OffsetDateTime.parse("2026-08-13T01:02:03Z");

        ThresholdInfoEvent threshold = new ThresholdInfoEvent(
                10L,
                List.of(new SensorRange(
                        "TEMPERATURE",
                        "C",
                        BigDecimal.valueOf(18),
                        BigDecimal.valueOf(25)
                )),
                occurredAt
        );

        SensorInfoUpsertEvent sensor = new SensorInfoUpsertEvent(
                10L,
                "배양실",
                "북쪽 선반",
                "MODEL-A",
                "온도 센서",
                "EUI-001",
                "TEMPERATURE",
                "C",
                occurredAt
        );

        service.publish(threshold, List.of(sensor));

        verify(publisher).publishEvent(threshold);
        verify(publisher).publishEvent(sensor);

        assertThat(sensor.occurredAt())
                .isEqualTo(threshold.occurredAt());
    }
}