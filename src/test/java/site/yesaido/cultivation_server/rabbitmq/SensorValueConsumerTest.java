package site.yesaido.cultivation_server.rabbitmq;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;
import site.yesaido.cultivation_server.sensor.service.InfluxService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.*;

class SensorValueConsumerTest {

    private final InfluxService influxService = mock(InfluxService.class);
    private final Channel channel = mock(Channel.class);
    private final SensorValueConsumer consumer = new SensorValueConsumer(influxService);
    private final SensorValueEvent event = new SensorValueEvent(
            "farm-a", "room-1", "model-x", "sensor-01", "eui-01",
            "TEMPERATURE", "°C", BigDecimal.valueOf(23.5), OffsetDateTime.of(2026, 8, 9, 12, 34, 56, 0, ZoneOffset.UTC), 42L
    );

    @Test
    void acknowledgesOnlyAfterInfluxWriteSucceeds() throws Exception {
        consumer.process(event, channel, 10L);

        verify(influxService).save(event);
        verify(channel).basicAck(10L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void sendsFailedWriteToDeadLetterWithoutAcknowledging() throws Exception {
        doThrow(new IllegalStateException("write failed"))
                .when(influxService).save(event);

        consumer.process(event, channel, 10L);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(10L, false, false);
    }
}
