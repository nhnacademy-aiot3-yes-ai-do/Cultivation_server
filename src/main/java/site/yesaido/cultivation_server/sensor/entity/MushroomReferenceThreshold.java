package site.yesaido.cultivation_server.sensor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceThresholdRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "mushroom_reference_threshold", uniqueConstraints = {@UniqueConstraint(columnNames = {"sensor_type_id", "mushroom_reference_id"})})
public class MushroomReferenceThreshold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sensor_type_id")
    private SensorType sensorType;

    @ManyToOne
    @JoinColumn(name = "mushroom_reference_id")
    private MushroomReference mushroomReference;

    @Column(name = "threshold_min", precision = 10, scale = 4)
    private BigDecimal thresholdMin;

    @Column(name = "threshold_max", precision = 10, scale = 4)
    private BigDecimal thresholdMax;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MushroomReferenceThreshold(SensorType sensorType, MushroomReference mushroomReference, BigDecimal thresholdMin, BigDecimal thresholdMax) {
        this.sensorType = sensorType;
        this.mushroomReference = mushroomReference;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;

        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void isUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    // MushroomReferenc에서 호출하는 관계 끊기용
    public void disconnect() {
        this.sensorType.getMushroomReferenceThresholds().remove(this);
    }

    public static MushroomReferenceThreshold create(SensorType sensorType, MushroomReference mushroomReference, MushroomReferenceThresholdRequest dto) {
        MushroomReferenceThreshold threshold = new MushroomReferenceThreshold(sensorType, mushroomReference, dto.thresholdMin(), dto.thresholdMax());

        sensorType.getMushroomReferenceThresholds().add(threshold);
        mushroomReference.getMushroomReferenceThresholds().add(threshold);

        return threshold;
    }

    public void setSensorType(SensorType sensorType) {
        this.sensorType.getMushroomReferenceThresholds().remove(this);
        this.sensorType = sensorType;
        isUpdate();
    }

    public void setThresholdMin(BigDecimal thresholdMin) {
        this.thresholdMin = thresholdMin;
        isUpdate();
    }

    public void setThresholdMax(BigDecimal thresholdMax) {
        this.thresholdMax = thresholdMax;
        isUpdate();
    }
}
