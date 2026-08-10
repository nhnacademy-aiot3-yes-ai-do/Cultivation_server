package site.yesaido.cultivation_server.rabbitmq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoUpsertEvent;
import site.yesaido.cultivation_server.rabbitmq.event.SensorType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorInfoUpsertProducerTest {

    private SensorInfoUpsertEvent event;

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    SensorInfoUpsertProducer producer;

    @BeforeEach
    void setUp() {
        event = new SensorInfoUpsertEvent(
                10L,
                "배양실",
                "북쪽",
                "MODEL-A",
                "온도센서1",
                "EUI-001",
                SensorType.TEMPERATURE,
                "C"
        );
    }

    @Test
    void sendsSensorInfoUpsertEvent() {

        producer.sendSensorInfo(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.SENSOR_EXCHANGE),
                eq(RabbitMQConstants.RULE_ENGINE_SENSOR_INFO_QUEUE),
                same(event),
                any(CorrelationData.class)
        );
    }
}