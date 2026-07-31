package site.yesaido.cultivation_server.sensor.dto.response;

import java.util.List;

public record SensorTypeInfoListResponse(
        List<SensorTypeInfoResponse> sensorTypeInfoResponses
) {
}
