package site.yesaido.cultivation_server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitPublisherConfigTest {

    private final RabbitPublisherConfig config =
            new RabbitPublisherConfig();

    @Test
    @DisplayName("RabbitTemplate에 mandatory와 Confirm/Return 콜백 등록")
    void configuresRabbitTemplate() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        RabbitTemplateCustomizer customizer =
                config.rabbitTemplateCustomizer();

        customizer.customize(rabbitTemplate);

        verify(rabbitTemplate).setMandatory(true);
        verify(rabbitTemplate).setConfirmCallback(any());
        verify(rabbitTemplate).setReturnsCallback(any());
    }


    @Test
    @DisplayName("Publisher Confirm 콜백의 ACK, NACK 처리")
    void handlesPublisherConfirm() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        ArgumentCaptor<RabbitTemplate.ConfirmCallback> callbackCaptor =
                ArgumentCaptor.forClass(
                        RabbitTemplate.ConfirmCallback.class
                );

        config.rabbitTemplateCustomizer().customize(rabbitTemplate);

        verify(rabbitTemplate)
                .setConfirmCallback(callbackCaptor.capture());

        RabbitTemplate.ConfirmCallback callback =
                callbackCaptor.getValue();

        CorrelationData correlationData =
                new CorrelationData("correlation-1");

        assertThatCode(() ->
                callback.confirm(correlationData, true, null)
        ).doesNotThrowAnyException();

        assertThatCode(() ->
                callback.confirm(correlationData, false, "broker nack")
        ).doesNotThrowAnyException();

        // correlationData가 없는 경우도 로깅 중 NPE가 발생하면 안 됨
        assertThatCode(() ->
                callback.confirm(null, false, "missing correlation")
        ).doesNotThrowAnyException();
    }


    @Test
    @DisplayName("Publisher Return 콜백 라우팅 실패 메시지 처리")
    void handlesReturnedMessage() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        ArgumentCaptor<RabbitTemplate.ReturnsCallback> callbackCaptor =
                ArgumentCaptor.forClass(
                        RabbitTemplate.ReturnsCallback.class
                );

        config.rabbitTemplateCustomizer().customize(rabbitTemplate);

        verify(rabbitTemplate)
                .setReturnsCallback(callbackCaptor.capture());

        ReturnedMessage returnedMessage = new ReturnedMessage(
                new Message(
                        "test-message".getBytes(StandardCharsets.UTF_8)
                ),
                312,
                "NO_ROUTE",
                "sensor.exchange",
                "unknown.routing-key"
        );

        assertThatCode(() ->
                callbackCaptor.getValue()
                        .returnedMessage(returnedMessage)
        ).doesNotThrowAnyException();
    }


}