package site.yesaido.cultivation_server.rabbitmq;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoDeleteEvent;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoUpsertEvent;

import java.util.UUID;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.RULE_ENGINE_SENSOR_INFO_QUEUE;
import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.SENSOR_EXCHANGE;

@Slf4j
@RequiredArgsConstructor
@Component
public class SensorInfoProducer {
    private final RabbitTemplate rabbitTemplate;

    /**
     * SensorInfoEvent 객체
     * → JSON body
     * → content_type = application/json
     * → __TypeId__ = sensor.upsert
     * → RabbitMQ 발행
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendSensorInfo(SensorInfoUpsertEvent event) {
        // 추적성 확보와 추후 확장성 때문에 사용
        CorrelationData correlationData =
                new CorrelationData(UUID.randomUUID().toString());

        try {
            rabbitTemplate.convertAndSend(
                    SENSOR_EXCHANGE, RULE_ENGINE_SENSOR_INFO_QUEUE, event, correlationData
            );

            log.info(
                    "SensorInfo publish requested: cultivationId={}, deviceEui={}, type={}, unit={}",
                    event.cultivationId(),
                    event.deviceEui(),
                    event.sensorType(),
                    event.unit()
            );
        } catch (AmqpException exception) {
            log.error(
                    "SensorInfo publish failed after retry: cultivationId={}, deviceEui={}, type={}, unit={}",
                    event.cultivationId(),
                    event.deviceEui(),
                    event.sensorType(),
                    event.unit(),
                    exception
            );

            throw exception;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendSensorDeleteInfo(SensorInfoDeleteEvent event) {
        rabbitTemplate.convertAndSend(SENSOR_EXCHANGE, RULE_ENGINE_SENSOR_INFO_QUEUE, event);
    }
}
