package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;

import java.util.List;

public interface InfluxService {


    LatestSensorValueListResponse findLatestByCultivationId(long cultivationId);

    List<SensorTypeAverageResponse> findAverageByCultivationIdForLast24Hours(long cultivationId);

    SensorTrendPointListResponse findTrend(long cultivationId, String deviceEui, String sensorType);

    List<SensorTypeAverageResponse> findAverageByCultivationId(long cultivationId);
}
