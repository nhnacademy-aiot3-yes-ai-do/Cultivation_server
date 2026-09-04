package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.sensor.dto.projection.CultivationSensorEuiProjection;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorLatestBatchServiceTest {
    @Mock
    private CultivationSensorRepository cultivationSensorRepository;

    @Mock
    private SensorRedisCacheService sensorRedisCacheService;

    @Mock
    private CultivationSensorEuiProjection firstSensor;

    @Mock
    private CultivationSensorEuiProjection secondSensor;

    @Test
    void findsLatestValuesOnlyForAccessibleCultivationSensors() {
        when(firstSensor.getCultivationId()).thenReturn(10L);
        when(firstSensor.getDeviceEui()).thenReturn("EUI-10");
        when(secondSensor.getCultivationId()).thenReturn(10L);
        when(secondSensor.getDeviceEui()).thenReturn("EUI-11");
        when(cultivationSensorRepository.findAllAccessibleSensorEuis(7L))
                .thenReturn(List.of(firstSensor, secondSensor));
        when(sensorRedisCacheService.findLatestBySensorEuis(anyMap(), any()))
                .thenReturn(Map.of(10L, List.<LatestSensorValueResponse>of()));

        Map<Long, List<LatestSensorValueResponse>> result =
                new SensorLatestBatchService(cultivationSensorRepository, sensorRedisCacheService)
                        .findLatestForUser(7L, Duration.ofSeconds(3));

        assertThat(result).containsKey(10L);
        verify(sensorRedisCacheService).findLatestBySensorEuis(
                Map.of(10L, Set.of("EUI-10", "EUI-11")), Duration.ofSeconds(3));
    }
}
