package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.rabbitmq.event.SensorType;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;

import java.util.List;

public interface InfluxService {

    void save(SensorValueEvent event);

    List<LatestSensorValueResponse> findLatestByCultivationId(long cultivationId);

    List<SensorTypeAverageResponse> findAverageByCultivationIdForLast24Hours(long cultivationId);

    SensorTrendPointListResponse findTrend(long cultivationId, String deviceEui, SensorType sensorType);
}
