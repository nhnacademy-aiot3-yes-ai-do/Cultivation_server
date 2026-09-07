package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;

import java.time.Instant;
import java.util.List;

public interface SensorConnectionService {

    void synchronize(long cultivationId,
                     List<LatestSensorValueResponse> points,
                     Instant checkedAt);
}
