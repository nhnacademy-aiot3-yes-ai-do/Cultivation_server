package site.yesaido.cultivation_server.sensor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "sensor_type", uniqueConstraints = {@UniqueConstraint(columnNames = {"type","value_unit"})})
public class SensorType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "value_unit")
    private String valueUnit;

    public SensorType(String type, String valueUnit) {
        this.type = type;
        this.valueUnit = valueUnit;

        this.cultivationSensorTypes = new HashSet<>();
        this.environmentSettings = new HashSet<>();
        this.mushroomReferenceThresholds = new HashSet<>();
    }

    @OneToMany(mappedBy = "sensorType")
    private Set<CultivationSensorType> cultivationSensorTypes;

    @OneToMany(mappedBy = "sensorType")
    private Set<EnvironmentSetting> environmentSettings;

    @OneToMany(mappedBy = "sensorType")
    private Set<MushroomReferenceThreshold> mushroomReferenceThresholds;
}
