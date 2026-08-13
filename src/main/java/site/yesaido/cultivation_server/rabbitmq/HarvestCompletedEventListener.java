package site.yesaido.cultivation_server.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import site.yesaido.cultivation_server.rabbitmq.event.HarvestCompletedEvent;

@Component
@RequiredArgsConstructor
public class HarvestCompletedEventListener {
    private final HarvestCompletedNotificationProducer producer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHarvestCompletedEvent(HarvestCompletedEvent event) {
        producer.send(event.cultivationId(), event.payload());
    }
}
