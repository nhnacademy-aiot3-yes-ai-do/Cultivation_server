package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;

import java.util.List;

public record CultivationMetadataResponse(
        CultivationDetailResponse cultivation,
        CultivationSensorListResponse sensors,
        MushroomReferenceInfoResponse mushroom,
        List<LatestSensorValueResponse> sensorHistory12h
) {
    public CultivationMetadataResponse {
        sensors = sensors == null
                ? new CultivationSensorListResponse(List.of(), List.of()) : sensors;
        sensorHistory12h = sensorHistory12h == null ? List.of() : List.copyOf(sensorHistory12h);
    }
}
