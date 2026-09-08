package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import site.yesaido.cultivation_server.rabbitmq.event.SensorDataUnavailableEvent;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorConnectStatus;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.service.impl.SensorConnectionServiceImpl;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorConnectionServiceTest {

    @Mock
    private CultivationSensorRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SensorConnectionServiceImpl service;

    @Test
    void checksNonDeletedSensorsOfRequestedCultivationEvenWithoutMeasurements() {
        long cultivationId = 17L;
        when(repository.findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(cultivationId))
                .thenReturn(List.of());

        service.synchronize(cultivationId, List.of(), Instant.now());

        verify(repository).findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(cultivationId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void marksOnlineSensorOfflineWhenNoMeasurementArrivesForThirtySeconds() {
        Instant checkedAt = Instant.now();
        Instant startedAt = checkedAt.minusSeconds(60);
        Instant lastMeasuredAt = checkedAt.minusSeconds(30);
        CultivationSensor sensor = mock(CultivationSensor.class);
        when(sensor.getId()).thenReturn(1L);
        when(sensor.getDeviceEui()).thenReturn("EUI-A");
        when(sensor.getMonitoringStartedAt()).thenReturn(startedAt);
        when(sensor.getLastMeasuredAt()).thenReturn(lastMeasuredAt);
        when(sensor.getSensorStatus()).thenReturn(SensorConnectStatus.ONLINE);
        when(repository.findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(17L))
                .thenReturn(List.of(sensor));
        when(sensor.getDeviceName()).thenReturn("온도센서");
        when(repository.updateConnectionIfUnchanged(
                1L, startedAt, "ONLINE", lastMeasuredAt, "OFFLINE", lastMeasuredAt
        )).thenReturn(1);

        service.synchronize(17L, List.of(), checkedAt);

        verify(repository).updateConnectionIfUnchanged(
                1L, startedAt, "ONLINE", lastMeasuredAt,
                "OFFLINE", lastMeasuredAt);

        verify(eventPublisher).publishEvent(argThat(
                (SensorDataUnavailableEvent event) ->
                        event.eventId() != null
                                && event.cultivationId() == 17L
                                && event.deviceName().equals("온도센서")
                                && event.occurredAt().toInstant().equals(checkedAt)
        ));
    }

    @Test
    void skipsSynchronizationWhenQueryResultIsOlderThanTenSeconds() {
        service.synchronize(
                17L, List.of(), Instant.now().minusSeconds(11));

        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void processesNextSensorWhenFirstSensorHasFutureTimestamp(boolean futureMeasurement) {
        Instant checkedAt = Instant.now();
        Instant startedAt = checkedAt.minusSeconds(60);
        Instant lastMeasuredAt = checkedAt.minusSeconds(30);
        CultivationSensor skipped = mock(CultivationSensor.class);
        when(skipped.getMonitoringStartedAt())
                .thenReturn(futureMeasurement ? startedAt : checkedAt.plusSeconds(1));
        when(skipped.getLastMeasuredAt())
                .thenReturn(futureMeasurement ? checkedAt.plusSeconds(1) : null);
        CultivationSensor next = onlineSensor(startedAt, lastMeasuredAt);
        when(repository.findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(17L))
                .thenReturn(List.of(skipped, next));

        service.synchronize(17L, List.of(), checkedAt);

        verify(repository).updateConnectionIfUnchanged(
                2L, startedAt, "ONLINE", lastMeasuredAt, "OFFLINE", lastMeasuredAt);
        verify(repository).findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(17L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void processesNextSensorWhenFirstSensorHasNoStateChange() {
        Instant checkedAt = Instant.now();
        Instant startedAt = checkedAt.minusSeconds(60);
        Instant lastMeasuredAt = checkedAt.minusSeconds(30);
        CultivationSensor waiting = mock(CultivationSensor.class);
        when(waiting.getMonitoringStartedAt()).thenReturn(startedAt);
        when(waiting.getDeviceEui()).thenReturn("EUI-WAITING");
        when(waiting.getSensorStatus()).thenReturn(SensorConnectStatus.OFFLINE);
        CultivationSensor next = onlineSensor(startedAt, lastMeasuredAt);
        when(repository.findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(17L))
                .thenReturn(List.of(waiting, next));

        service.synchronize(17L, List.of(), checkedAt);

        verify(repository).updateConnectionIfUnchanged(
                2L, startedAt, "ONLINE", lastMeasuredAt, "OFFLINE", lastMeasuredAt);
        verify(repository).findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(17L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(eventPublisher);
    }

    private CultivationSensor onlineSensor(Instant startedAt, Instant lastMeasuredAt) {
        CultivationSensor sensor = mock(CultivationSensor.class);
        when(sensor.getId()).thenReturn(2L);
        when(sensor.getDeviceEui()).thenReturn("EUI-NEXT");
        when(sensor.getMonitoringStartedAt()).thenReturn(startedAt);
        when(sensor.getLastMeasuredAt()).thenReturn(lastMeasuredAt);
        when(sensor.getSensorStatus()).thenReturn(SensorConnectStatus.ONLINE);
        return sensor;
    }
}
