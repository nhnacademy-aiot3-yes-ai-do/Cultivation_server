package site.yesaido.cultivation_server.entity.environmentsetting;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "environment_setting")
public class EnvironmentSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "temp_min", nullable = false)
    private Double tempMin;
    @Column(name = "temp_max", nullable = false)
    private Double tempMax;

    @Column(name = "humidity_min", nullable = false)
    private Double humidityMin;
    @Column(name = "humidity_max", nullable = false)
    private Double humidityMax;

    @Column(name = "co2_min", nullable = false)
    private Integer co2Min;
    @Column(name = "co2_max", nullable = false)
    private Integer co2Max;

    @Column(name = "light_min", nullable = false)
    private Integer lightMin;
    @Column(name = "light_max", nullable = false)
    private Integer lightMax;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cultivation_id", nullable = false, unique = true)
    private Cultivation cultivation;
}
