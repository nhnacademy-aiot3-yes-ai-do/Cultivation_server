package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.service.SensorConnectionService;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorConnectionServiceImpl implements SensorConnectionService {

    private final CultivationSensorRepository cultivationSensorRepository;

    @Transactional
    @Override
    public void synchronize(long cultivationId, List<LatestSensorValueResponse> points, Instant checkedAt) {
        List<CultivationSensor> cultivationSensors = cultivationSensorRepository
                .findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(cultivationId);

        log.debug("{}", cultivationSensors);


    }
}
