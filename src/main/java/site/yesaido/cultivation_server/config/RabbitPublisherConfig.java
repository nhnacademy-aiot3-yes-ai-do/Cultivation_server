package site.yesaido.cultivation_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class RabbitPublisherConfig {

    /**
     * 1. RabbitTemplate 3회 재시도
     * 2. Publisher Confirm/Return 로깅
     * 3. OpenObserve에서 publish NACK/Return ERROR 알림
     * 4. 추후 필요하면 Outbox 도입
     */
    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer() {
        return rabbitTemplate -> {
            rabbitTemplate.setMandatory(true);

            rabbitTemplate.setConfirmCallback(
                    (correlationData, ack, cause) -> {
                        if (ack) {
                            log.debug("RabbitMQ publish ACK: correlationId={}",
                                    correlationData != null
                                            ? correlationData.getId()
                                            : null
                            );
                            return;
                        }

                        log.error(
                                "RabbitMQ publish NACK: correlationId={}, cause={}",
                                correlationData != null
                                        ? correlationData.getId()
                                        : null,
                                cause
                        );
                    }
            );

            rabbitTemplate.setReturnsCallback(returned ->
                    log.error("RabbitMQ message returned: exchange={}, routingKey={}, replyCode={}, replyText={}",
                            returned.getExchange(),
                            returned.getRoutingKey(),
                            returned.getReplyCode(),
                            returned.getReplyText()
                    )
            );
        };
    }
}
