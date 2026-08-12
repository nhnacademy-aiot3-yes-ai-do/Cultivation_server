package site.yesaido.cultivation_server.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import site.yesaido.cultivation_server.rabbitmq.event.MemberAddedPayload;
import site.yesaido.cultivation_server.rabbitmq.event.NotificationEvent;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.NOTIFICATION_DONE_QUEUE;
import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.NOTIFICATION_EXCHANGE;

@Component
@RequiredArgsConstructor
public class MemberAddedNotificationProducer {
    private static final String PRODUCER = "cultivation-server";
    private static final String EVENT_TYPE = "MEMBER_ADDED";
    private static final String TARGET_TYPE = "USER";
    private final RabbitTemplate rabbitTemplate;

    public void send(Long addedUserId, MemberAddedPayload payload) {
        NotificationEvent<MemberAddedPayload> event = new NotificationEvent<>(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                PRODUCER,
                TARGET_TYPE,
                addedUserId,
                OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9)).toString(),
                payload
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, NOTIFICATION_DONE_QUEUE, event);
    }
}
