package site.yesaido.cultivation_server.entity.sensor;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "sensor")
public class Sensor {
    @Id
    @Column(name = "device_eui", length = 32)
    private String id;

    @Column(length = 50)
    private String place;

    @Column(length = 50)
    private String location;

    @Column(length = 100)
    private String deviceModel;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private SensorType sensorType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
    private Status status = Status.ONLINE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cultivation_id", nullable = false)
    private Cultivation cultivation;
}
