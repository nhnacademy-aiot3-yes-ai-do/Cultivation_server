package site.yesaido.cultivation_server.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import site.yesaido.cultivation_server.rabbitmq.event.MemberAddedPayload;
import site.yesaido.cultivation_server.rabbitmq.event.NotificationEvent;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.NOTIFICATION_EXCHANGE;
import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.NOTIFICATION_HARVEST_ROUTING_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberAddedNotificationProducer {
    private static final String PRODUCER = "cultivation-server";
    private static final String EVENT_TYPE = "MEMBER_ADDED";
    private static final String TARGET_TYPE = "USER";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MILLIS = 100;
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
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, NOTIFICATION_HARVEST_ROUTING_KEY, event);
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("MEMBER_ADDED 알림 발행 실패, best-effort 정책에 따라 포기함: eventId={}, addedUserId={}, cultivationId={}, attempts={}, cause={}",
                            event.eventId(), addedUserId, payload.cultivationId(), attempt, e.getMessage());
                    return;
                }
                log.warn("MEMBER_ADDED 알림 발행 실패, 재시도함: eventId={}, attempt={}/{}, cause={}",
                        event.eventId(), attempt, MAX_ATTEMPTS, e.getMessage());
                sleepBeforeRetry();
            }
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
