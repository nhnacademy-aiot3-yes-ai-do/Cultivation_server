package site.yesaido.cultivation_server.rabbitmq.event;

public record HarvestCompletedEvent(
        Long cultivationId,
        Long userId,
        HarvestCompletedPayload payload) {
}
