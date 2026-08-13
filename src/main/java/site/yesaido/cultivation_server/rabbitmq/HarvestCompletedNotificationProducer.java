package site.yesaido.cultivation_server.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import site.yesaido.cultivation_server.rabbitmq.event.HarvestCompletedPayload;
import site.yesaido.cultivation_server.rabbitmq.event.NotificationEvent;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.NOTIFICATION_DONE_QUEUE;
import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.NOTIFICATION_EXCHANGE;

@Slf4j
@Component
@RequiredArgsConstructor
public class HarvestCompletedNotificationProducer {
    private static final String PRODUCER = "cultivation-server";
    private static final String EVENT_TYPE = "HARVEST_COMPLETED";
    private static final String TARGET_TYPE = "CULTIVATION";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MILLIS = 100;
    private final RabbitTemplate rabbitTemplate;

    public void send(Long cultivationId, HarvestCompletedPayload payload) {
        NotificationEvent<HarvestCompletedPayload> event = new NotificationEvent<>(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                PRODUCER,
                TARGET_TYPE,
                cultivationId,
                OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9)).toString(),
                payload
        );
        for (int attempt = 0; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, NOTIFICATION_DONE_QUEUE, event);
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("HARVEST_COMPLETED 알림 발행 실패, best-effort 정책에 따라 포기함: eventId={}, cultivationId={}, attempts={}, cause={}",
                            event.eventId(), cultivationId, attempt + 1, e.getMessage());
                    return;
                }
                log.warn("HARVEST_COMPLETED 알림 발행 실패, 재시도함: eventId={}, attempt={}/{}, cause={}",
                        event.eventId(), attempt, MAX_ATTEMPTS, e.getMessage());
                sleepBeforeRetry();
            }
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
