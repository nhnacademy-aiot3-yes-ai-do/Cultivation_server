package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;

import java.util.List;

public interface InfluxService {


    List<LatestSensorValueResponse> findLatestByCultivationId(long cultivationId);

    List<SensorTypeAverageResponse> findAverageByCultivationIdForLast24Hours(long cultivationId);

    SensorTrendPointListResponse findTrend(long cultivationId, String deviceEui, String sensorType);
}
