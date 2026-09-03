package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface InfluxService {

    // 캐시 워밍업/폴링용 최근 원시 포인트
    List<LatestSensorValueResponse> findValuesByCultivationId(long cultivationId, Duration range);

    // page-data fallback용 시간 구간 평균 포인트
    List<LatestSensorValueResponse> findAveragedValuesByCultivationId(long cultivationId, Duration range);


    // 센서·단위·장치별 최신값
    LatestSensorValueListResponse findLatestByCultivationId(long cultivationId);

    Map<Long, List<LatestSensorValueResponse>> findLatestByCultivationIds(List<Long> cultivationIds);

    // 최근 24시간 센서 종류, 단위는 통합한 평균
    List<SensorTypeAverageResponse> findAverageByCultivationIdForLast24Hours(long cultivationId);

    // 특정 장치와 센서 종류의 최근 24시간 데이터를 15분 평균으로 반환
    SensorTrendPointListResponse findTrend(long cultivationId, String deviceEui, String sensorType, String unit);

    List<SensorTypeAverageResponse> findAverageByCultivationId(long cultivationId);
}
