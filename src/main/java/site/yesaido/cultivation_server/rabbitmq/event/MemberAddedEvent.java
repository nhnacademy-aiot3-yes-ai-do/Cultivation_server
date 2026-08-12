package site.yesaido.cultivation_server.rabbitmq.event;

public record MemberAddedEvent(Long addedUserId, MemberAddedPayload payload) {
}
