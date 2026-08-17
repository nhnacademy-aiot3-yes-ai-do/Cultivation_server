package site.yesaido.cultivation_server.rabbitmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.rabbitmq.event.EnvironmentComplianceRequest;
import site.yesaido.cultivation_server.sensor.service.EnvironmentComplianceService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnvironmentComplianceRpcListenerTest {

    @Mock
    private EnvironmentComplianceService environmentComplianceService;

    @InjectMocks
    private EnvironmentComplianceRpcListener listener;

    @Test
    @DisplayName("RPC 요청을 서비스에 위임하고 응답을 그대로 반환한다")
    void handleDelegatesToServiceAndReturnsResponse() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 17);
        EnvironmentComplianceRequest request = new EnvironmentComplianceRequest(1L, startDate, endDate);
        EnvironmentComplianceResponse response = new EnvironmentComplianceResponse(
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(95), BigDecimal.valueOf(80));
        given(environmentComplianceService.getComplianceForPeriod(1L, startDate, endDate)).willReturn(response);

        EnvironmentComplianceResponse result = listener.handle(request);

        assertThat(result).isEqualTo(response);
        verify(environmentComplianceService).getComplianceForPeriod(1L, startDate, endDate);
    }

    @Test
    @DisplayName("서비스에서 예외가 발생하면 로그를 남기고 그대로 재전파한다")
    void handlePropagatesExceptionFromService() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 17);
        EnvironmentComplianceRequest request = new EnvironmentComplianceRequest(1L, startDate, endDate);
        RuntimeException failure = new RuntimeException("조회 실패");
        given(environmentComplianceService.getComplianceForPeriod(1L, startDate, endDate)).willThrow(failure);

        assertThatThrownBy(() -> listener.handle(request)).isSameAs(failure);
    }
}