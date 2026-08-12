package site.yesaido.cultivation_server.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import site.yesaido.cultivation_server.rabbitmq.event.MemberAddedEvent;
import site.yesaido.cultivation_server.rabbitmq.event.MemberAddedPayload;

@Component
@RequiredArgsConstructor
public class MemberAddedEventListner {
    private final MemberAddedNotificationProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberAddedEvent(MemberAddedEvent event) {
        producer.send(event.addedUserId(), event.payload());
    }
}
