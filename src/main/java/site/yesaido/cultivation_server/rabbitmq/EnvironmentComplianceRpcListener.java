package site.yesaido.cultivation_server.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.rabbitmq.event.EnvironmentComplianceRequest;
import site.yesaido.cultivation_server.sensor.service.EnvironmentComplianceService;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentComplianceRpcListener {
    // Rpc는 원격에 있는 함수를 마치 내 컴퓨터와 함수처럼 호출하는 방식
    private final EnvironmentComplianceService environmentComplianceService;

    @RabbitListener(queues = ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE)
    public EnvironmentComplianceResponse handle(EnvironmentComplianceRequest request) {
        try {
            return environmentComplianceService.getComplianceForPeriod(
                    request.cultivationId(), request.startDate(), request.endDate());
        } catch (Exception e) {
            log.warn("환경유지율 RPC 처리 실패: cultivationId={}, startDate={}, endDate={}",
                    request.cultivationId(), request.startDate(), request.endDate(), e);
            throw e;
        }
    }
}
