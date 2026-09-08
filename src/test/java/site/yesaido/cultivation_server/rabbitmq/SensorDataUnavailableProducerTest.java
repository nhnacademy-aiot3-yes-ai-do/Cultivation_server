package site.yesaido.cultivation_server.rabbitmq;

import tools.jackson.databind.json.JsonMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import site.yesaido.cultivation_server.rabbitmq.event.SensorDataUnavailableEvent;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SensorDataUnavailableProducerTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void retriesWithSameEventId(boolean exhaustRetries) throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        Channel channel = mock(Channel.class);
        when(factory.createConnection()).thenReturn(connection);
        when(connection.createChannel(false)).thenReturn(channel);

        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(new JacksonJsonMessageConverter());
        template.setRetryTemplate(new RetryTemplate(
                RetryPolicy.builder().maxRetries(3).delay(Duration.ZERO).build()));

        List<byte[]> sentBodies = new ArrayList<>();
        doAnswer(invocation -> {
            sentBodies.add(invocation.getArgument(4));
            if (exhaustRetries || sentBodies.size() <= 3) {
                throw new IOException("전송 실패");
            }
            return null;
        }).when(channel).basicPublish(
                eq(RabbitMQConstants.NOTIFICATION_EXCHANGE),
                eq("yes-nhn.notification.sensor.queue"),
                anyBoolean(), any(), any(byte[].class));

        SensorDataUnavailableEvent event = new SensorDataUnavailableEvent(
                UUID.randomUUID(), 17L, "온도센서", "수신 중단", OffsetDateTime.now());
        SensorDataUnavailableProducer producer = new SensorDataUnavailableProducer(template);

        if (exhaustRetries) {
            assertThatThrownBy(() -> producer.send(event)).isInstanceOf(AmqpException.class);
        } else {
            assertThatCode(() -> producer.send(event)).doesNotThrowAnyException();
        }
        var mapper = JsonMapper.builder().build();
        assertThat(sentBodies).hasSize(4).allSatisfy(body ->
                assertThat(mapper.readTree(body).path("eventId").asString())
                        .isEqualTo(event.eventId().toString()));
    }
}