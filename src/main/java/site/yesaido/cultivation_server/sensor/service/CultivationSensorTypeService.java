package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.util.List;

public interface CultivationSensorTypeService {

    void syncSensorTypes(CultivationSensor sensor, List<SensorType> sensorTypes);
}
