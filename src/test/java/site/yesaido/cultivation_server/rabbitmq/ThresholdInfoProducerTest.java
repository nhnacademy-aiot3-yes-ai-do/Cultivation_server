package site.yesaido.cultivation_server.rabbitmq;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ThresholdInfoProducerTest {
    private ThresholdInfoEvent event;

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    ThresholdInfoProducer producer;

    @BeforeEach
    void setUp() {
        event = new ThresholdInfoEvent(
                10L,
                List.of(),
                OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9))
        );
    }

    @Test
    void sendsThresholdInfoEvent() {
        producer.sendThresholdInfo(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.SENSOR_EXCHANGE),
                eq(RabbitMQConstants.RULE_ENGINE_THRESHOLD_INFO_QUEUE),
                same(event),
                any(CorrelationData.class)
        );
    }


}