package site.yesaido.cultivation_server.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.rabbitmq.event.EnvironmentComplianceRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE;
import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.SENSOR_EXCHANGE;

@SpringBootTest
class EnvironmentComplianceRpcManualTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void requestReplyRoundTrip() {
        rabbitTemplate.setReplyTimeout(15000);

        EnvironmentComplianceRequest request = new EnvironmentComplianceRequest(
                4L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7));

        Object reply = rabbitTemplate.convertSendAndReceive(SENSOR_EXCHANGE, ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE, request);

        System.out.println("응답: " + reply);
        assertThat(reply).isInstanceOf(EnvironmentComplianceResponse.class);
    }
}