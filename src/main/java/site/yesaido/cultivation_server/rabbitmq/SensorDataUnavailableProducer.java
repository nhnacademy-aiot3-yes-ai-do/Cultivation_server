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
import site.yesaido.cultivation_server.rabbitmq.event.SensorDataUnavailableEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataUnavailableProducer {
    private final RabbitTemplate rabbitTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(SensorDataUnavailableEvent event) {

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.NOTIFICATION_EXCHANGE,
                    "yes-nhn.notification.sensor.queue",
                    event,
                    new CorrelationData(event.eventId().toString())
            );
        } catch (AmqpException e) {
            log.error("Sensor offline publish failed: eventId={}, cultivationId={}",
                    event.eventId(), event.cultivationId(), e);
            throw e;
        }

    }
}
