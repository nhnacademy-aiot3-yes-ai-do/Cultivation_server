package site.yesaido.cultivation_server.sensor.dto.response;

import java.util.List;

public record ReusableCultivationSensorListResponse(
        List<ReusableCultivationSensorResponse> sensors
) {
    public ReusableCultivationSensorListResponse {
        sensors = sensors == null ? List.of() : List.copyOf(sensors);
    }
}