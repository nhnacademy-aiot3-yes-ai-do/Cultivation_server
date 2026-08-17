package site.yesaido.cultivation_server.sensor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cultivation_sensor", uniqueConstraints = {@UniqueConstraint(columnNames = {"cultivation_id", "device_eui"})})
public class CultivationSensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cultivation_id")
    private long cultivationId;

    @Column(name = "device_eui")
    private String deviceEui;

    @Column(name = "device_model")
    private String deviceModel;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "location")
    private String location;

    @Column(name = "location_detail")
    private String locationDetail;

    @Column(name = "sensor_status")
    @Enumerated(EnumType.STRING)
    private SensorConnectStatus sensorStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    public CultivationSensor(long cultivationId, String deviceEui, String deviceModel, String deviceName, String location, String locationDetail) {
        this.cultivationId = cultivationId;
        this.deviceEui = deviceEui;
        this.deviceModel = deviceModel;
        this.deviceName = deviceName;
        this.location = location;
        this.locationDetail = locationDetail;

        this.sensorStatus = SensorConnectStatus.OFFLINE;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        this.cultivationSensorTypes = new HashSet<>();
    }

    @OneToMany(mappedBy = "cultivationSensor")
    private Set<CultivationSensorType> cultivationSensorTypes;

    public void toDelete() {
        this.isDeleted = true;
        this.sensorStatus = SensorConnectStatus.OFFLINE;
    }

    public void toRestore(
            String deviceModel,
            String deviceName,
            String location,
            String locationDetail
    ) {
        this.deviceModel = deviceModel;
        this.deviceName = deviceName;
        this.location = location;
        this.locationDetail = locationDetail;
        this.sensorStatus = SensorConnectStatus.OFFLINE;
        this.isDeleted = false;
    }
}
