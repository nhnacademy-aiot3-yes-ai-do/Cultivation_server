package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.service.impl.SensorConnectionServiceImpl;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorConnectionServiceTest {

    @Mock
    private CultivationSensorRepository repository;

    @InjectMocks
    private SensorConnectionServiceImpl service;

    @Test
    void checksNonDeletedSensorsOfRequestedCultivationEvenWithoutMeasurements() {
        long cultivationId = 17L;
        when(repository.findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(cultivationId))
                .thenReturn(List.of());

        service.synchronize(cultivationId, List.of(), Instant.parse("2026-09-07T00:00:00Z"));

        verify(repository).findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(cultivationId);
        verifyNoMoreInteractions(repository);
    }
}
