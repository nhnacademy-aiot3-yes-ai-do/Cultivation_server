package site.yesaido.cultivation_server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.MessageConverter;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class RabbitMessageConverterConfigTest {

    private final RabbitMessageConverterConfig config = new RabbitMessageConverterConfig();

    // 현재 사용중인 Jackson 3은 Java Time 처리를 기본 지원
    @Test
    @DisplayName("java Time처리 및 역직렬화 TypeId 매핑 테스트")
    void serializesAndDeserializesOffsetDateTimeAsIso8601() {
        DefaultClassMapper classMapper = config.classMapper();

        // 직접 생성한 객체이므로 Spring 대신 초기화 수행
        classMapper.afterPropertiesSet();

        MessageConverter converter = config.jsonMessageConverter(classMapper);

        OffsetDateTime occurredAt =
                OffsetDateTime.parse("2026-08-13T01:02:03.456Z");

        ThresholdInfoEvent event = new ThresholdInfoEvent(
                1L,
                List.of(),
                occurredAt
        );

        Message message = converter.toMessage(
                event,
                new MessageProperties()
        );

        String json = new String(
                message.getBody(),
                StandardCharsets.UTF_8
        );

        String typeId = message.getMessageProperties()
                .getHeader("__TypeId__");

        assertThat(typeId)
                .isEqualTo("threshold.crud");

        assertThat(json).contains("\"occurredAt\":\"2026-08-13T01:02:03.456Z\"");

        Object restored = converter.fromMessage(message);

        assertThat(restored)
                .isInstanceOf(ThresholdInfoEvent.class);

        ThresholdInfoEvent restoredEvent =
                (ThresholdInfoEvent) restored;

        assertThat(restoredEvent.occurredAt())
                .isEqualTo(occurredAt);

    }
}
