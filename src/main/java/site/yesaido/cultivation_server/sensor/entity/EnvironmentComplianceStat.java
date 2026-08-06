package site.yesaido.cultivation_server.sensor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "environment_compliance_stat", uniqueConstraints = {@UniqueConstraint(columnNames = {"cultivation_id", "sensor_type_id"})})
public class EnvironmentComplianceStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cultivation_id")
    private Long cultivationId;

    @Column(name = "in_range_count")
    private Integer inRangeCount;

    @Column(name = "out_of_range_count")
    private Integer outOfRangeCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "sensor_type_id")
    private SensorType sensorType;

    public EnvironmentComplianceStat(long cultivationId, SensorType sensorType) {
        this.cultivationId = cultivationId;
        this.sensorType = sensorType;
        this.inRangeCount = 0;
        this.outOfRangeCount = 0;
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}

