package site.yesaido.cultivation_server.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.RULE_ENGINE_THRESHOLD_INFO_QUEUE;
import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.SENSOR_EXCHANGE;

@RequiredArgsConstructor
@Component
public class ThresholdInfoProducer {
    private final RabbitTemplate rabbitTemplate;

    @Async
    @TransactionalEventListener
    public void sendThresholdInfo(ThresholdInfoEvent event) {
        rabbitTemplate.convertAndSend(SENSOR_EXCHANGE, RULE_ENGINE_THRESHOLD_INFO_QUEUE, event);
    }
}
