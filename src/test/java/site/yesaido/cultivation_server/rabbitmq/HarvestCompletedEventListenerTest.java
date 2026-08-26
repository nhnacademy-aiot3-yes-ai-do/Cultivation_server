package site.yesaido.cultivation_server.rabbitmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.rabbitmq.event.HarvestCompletedEvent;
import site.yesaido.cultivation_server.rabbitmq.event.HarvestCompletedPayload;

import java.math.BigDecimal;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HarvestCompletedEventListenerTest {

    @Mock
    private HarvestCompletedNotificationProducer producer;

    @InjectMocks
    private HarvestCompletedEventListener listener;

    @Test
    @DisplayName("이벤트 수신 시 producer.send를 정확한 인자로 위임함")
    void handleHarvestCompletedEventDelegatesToProducer() {
        HarvestCompletedPayload payload = new HarvestCompletedPayload("테스트 경작", BigDecimal.valueOf(1200));
        HarvestCompletedEvent event = new HarvestCompletedEvent(200L, 1L, payload);

        listener.handleHarvestCompletedEvent(event);

        verify(producer, times(1)).send(200L, 1L, payload);
    }
}