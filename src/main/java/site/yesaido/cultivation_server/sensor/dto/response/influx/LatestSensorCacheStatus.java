package site.yesaido.cultivation_server.sensor.dto.response.influx;

public enum LatestSensorCacheStatus {
    FRESH,
    PARTIAL,
    SOURCE_FALLBACK,
    NO_DATA,
    REDIS_PENDING
}
