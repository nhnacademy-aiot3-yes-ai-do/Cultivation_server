package site.yesaido.cultivation_server.sensor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.service.SensorLatestBatchService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

class SensorValueBatchControllerTest {
    private final SensorLatestBatchService service = mock(SensorLatestBatchService.class);
    private final SensorValueBatchController controller = new SensorValueBatchController(service);

    @Test
    void returnsLatestValuesGroupedByCultivationId() {
        LatestSensorValueResponse point = new LatestSensorValueResponse(
                10L, "temperature", "C", BigDecimal.valueOf(22.5), null,
                "EUI-10", "model", "name", "location", "place"
        );
        when(service.findLatestForUser(7L, Duration.ZERO))
                .thenReturn(Map.of(10L, List.of(point)));

        var response = controller.getLatestForUser(7L);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody().latestSensorValuesByCultivationId())
                .containsEntry(10L, List.of(point));
    }

    @Test
    void doesNotMaskUnexpectedApplicationErrorsAsRedisFailure() {
        when(service.findLatestForUser(7L, Duration.ZERO))
                .thenThrow(new IllegalStateException("application failure"));

        assertThatThrownBy(() -> controller.getLatestForUser(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("application failure");
    }

    @Test
    void returnsServiceUnavailableWhenRedisBatchFails() {
        when(service.findLatestForUser(7L, Duration.ZERO))
                .thenThrow(new DataAccessException("redis unavailable") { });

        var response = controller.getLatestForUser(7L);

        assertThat(response.getStatusCode()).isEqualTo(SERVICE_UNAVAILABLE);
        assertThat(response.getBody().latestSensorValuesByCultivationId()).isEmpty();
    }
}
