package site.yesaido.cultivation_server.sensor.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "environment_compliance_stat", uniqueConstraints = {@UniqueConstraint(columnNames = {"cultivation_id", "sensor_type_id", "stat_date"})})
public class EnvironmentComplianceStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cultivation_id", nullable = false)
    private Long cultivationId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "in_range_count", nullable = false)
    private Integer inRangeCount;

    @Column(name = "out_of_range_count", nullable = false)
    private Integer outOfRangeCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sensor_type_id", nullable = false)
    private SensorType sensorType;

    public EnvironmentComplianceStat(long cultivationId, SensorType sensorType, LocalDate statDate) {
        this.cultivationId = cultivationId;
        this.sensorType = sensorType;
        this.statDate = statDate;
        this.inRangeCount = 0;
        this.outOfRangeCount = 0;
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}

