package site.yesaido.cultivation_server.rabbitmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.rabbitmq.event.MemberAddedEvent;
import site.yesaido.cultivation_server.rabbitmq.event.MemberAddedPayload;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberAddedEventListenerTest {

    @Mock
    private MemberAddedNotificationProducer producer;

    @InjectMocks
    private MemberAddedEventListner listener;

    @Test
    @DisplayName("이벤트 수신 시 producer.send를 정확한 인자로 위임함 - producer 내부 실패와 무관하게 리스너는 항상 정상 종료됨(best-effort)")
    void handleMemberAddedEventDelegatesToProducer() {
        MemberAddedPayload payload = new MemberAddedPayload(1L, "테스트 경작", MemberRole.MEMBER);
        MemberAddedEvent event = new MemberAddedEvent(200L, payload);

        listener.handleMemberAddedEvent(event);

        verify(producer, times(1)).send(200L, payload);
    }
}