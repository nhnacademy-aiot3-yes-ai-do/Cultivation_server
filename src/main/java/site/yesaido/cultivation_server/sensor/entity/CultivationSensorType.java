package site.yesaido.cultivation_server.sensor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cultivation_sensor_type", uniqueConstraints = {@UniqueConstraint(columnNames = {"cultivation_sensor_id", "sensor_type_id"})})
public class CultivationSensorType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cultivation_sensor_id")
    private CultivationSensor cultivationSensor;

    @ManyToOne
    @JoinColumn(name = "sensor_type_id")
    private SensorType sensorType;

    public CultivationSensorType(CultivationSensor cultivationSensor, SensorType sensorType) {
        this.cultivationSensor = cultivationSensor;
        this.sensorType = sensorType;
    }
}
