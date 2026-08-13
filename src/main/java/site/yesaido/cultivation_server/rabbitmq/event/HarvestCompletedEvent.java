package site.yesaido.cultivation_server.rabbitmq.event;

public record HarvestCompletedEvent(Long cultivationId, HarvestCompletedPayload payload) {
}
