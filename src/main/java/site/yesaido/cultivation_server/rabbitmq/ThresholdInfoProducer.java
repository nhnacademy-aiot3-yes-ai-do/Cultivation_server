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
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;

import java.util.UUID;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.RULE_ENGINE_THRESHOLD_INFO_QUEUE;
import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.SENSOR_EXCHANGE;

@Slf4j
@RequiredArgsConstructor
@Component
public class ThresholdInfoProducer {
    private final RabbitTemplate rabbitTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendThresholdInfo(ThresholdInfoEvent event) {
        CorrelationData correlationData =
                new CorrelationData(UUID.randomUUID().toString());

        try {
            // RuleEngine 뿐만아닌 DataSource에도 전송됨
            rabbitTemplate.convertAndSend(SENSOR_EXCHANGE, RULE_ENGINE_THRESHOLD_INFO_QUEUE, event, correlationData);

            log.info(
                    "ThresholdInfo publish requested: cultivationId={}, occurredAt={}",
                    event.cultivationId(), event.occurredAt()
            );
        }
        catch (AmqpException exception) {
            log.error(
                    "ThresholdInfo publish failed after retry: cultivationId={}, occurredAt={}",
                    event.cultivationId(),
                    event.occurredAt(),
                    exception
            );

            throw exception;
        }
    }
}
