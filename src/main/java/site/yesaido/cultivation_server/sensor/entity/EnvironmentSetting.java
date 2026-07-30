package site.yesaido.cultivation_server.sensor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "environment_setting", uniqueConstraints = {@UniqueConstraint(columnNames = {"cultivation_id", "sensor_type_id"})})
public class EnvironmentSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cultivation_id")
    private long cultivationId;

    @ManyToOne
    @JoinColumn(name = "sensor_type_id")
    private SensorType sensorType;

    @Column(name = "threshold_min", precision = 10, scale = 4)
    private BigDecimal thresholdMin;

    @Column(name = "threshold_max", precision = 10, scale = 4)
    private BigDecimal thresholdMax;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public EnvironmentSetting(long cultivationId, SensorType sensorType, BigDecimal thresholdMin, BigDecimal thresholdMax) {
        this.cultivationId = cultivationId;
        this.sensorType = sensorType;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;

        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void idUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
