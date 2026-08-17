package site.yesaido.cultivation_server.rabbitmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import site.yesaido.cultivation_server.rabbitmq.event.HarvestCompletedPayload;
import site.yesaido.cultivation_server.rabbitmq.event.NotificationEvent;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HarvestCompletedNotificationProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private HarvestCompletedNotificationProducer producer;

    @Test
    @DisplayName("정상 발행 - 1회만 시도됨")
    void sendPublishesOnFirstAttempt() {
        HarvestCompletedPayload payload = new HarvestCompletedPayload("테스트 경작", BigDecimal.valueOf(1200));

        producer.send(200L, payload);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    @DisplayName("일시적 장애 - 재시도 후 성공하면 예외 없이 종료됨")
    void sendRetriesOnTransientFailureThenSucceeds() {
        HarvestCompletedPayload payload = new HarvestCompletedPayload("테스트 경작", BigDecimal.valueOf(1200));

        doThrow(new AmqpException("일시적 브로커 장애"))
                .doNothing()
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        assertThatCode(() -> producer.send(200L, payload)).doesNotThrowAnyException();

        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    @DisplayName("최대 재시도 초과 - 예외를 삼키고 best-effort로 포기함")
    void sendGivesUpAfterMaxAttemptsWithoutThrowing() {
        HarvestCompletedPayload payload = new HarvestCompletedPayload("테스트 경작", BigDecimal.valueOf(1200));

        doThrow(new AmqpException("브로커 다운"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        assertThatCode(() -> producer.send(200L, payload)).doesNotThrowAnyException();

        verify(rabbitTemplate, times(4)).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }
}