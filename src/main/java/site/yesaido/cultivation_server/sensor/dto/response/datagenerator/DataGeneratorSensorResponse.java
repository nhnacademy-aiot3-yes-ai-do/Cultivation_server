package site.yesaido.cultivation_server.sensor.dto.response.datagenerator;

import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

import java.util.Comparator;
import java.util.List;

// Data Generator에 전달할 센서 장치와 측정 채널 목록을 표현합니다.
public record DataGeneratorSensorResponse(
        long cultivationId,
        String deviceEui,
        String deviceName,
        String location,
        String locationDetail,
        String deviceModel,
        List<DataGeneratorSensorTypeResponse> sensorTypes
) {

    public static DataGeneratorSensorResponse from(
            CultivationSensor cultivationSensor
    ) {
        List<DataGeneratorSensorTypeResponse> sensorTypes = cultivationSensor.getCultivationSensorTypes().stream()
                        .map(cultivationSensorType -> DataGeneratorSensorTypeResponse
                                .from(cultivationSensorType.getSensorType()))
                        .sorted(Comparator.comparing(DataGeneratorSensorTypeResponse::sensorType)
                                .thenComparing(DataGeneratorSensorTypeResponse::unit))
                        .toList();

        return new DataGeneratorSensorResponse(
                cultivationSensor.getCultivationId(),
                cultivationSensor.getDeviceEui(),
                cultivationSensor.getDeviceName(),
                cultivationSensor.getLocation(),
                cultivationSensor.getLocationDetail(),
                cultivationSensor.getDeviceModel(),
                sensorTypes
        );
    }
}