package site.yesaido.cultivation_server.rabbitmq.event;

public record NotificationEvent<T>(
        String eventId,
        String eventType,
        String producer,
        String targetType,
        Long targetId,
        String occurredAt,
        T payload
) {
}
