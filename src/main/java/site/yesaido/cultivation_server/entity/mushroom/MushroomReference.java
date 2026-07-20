package site.yesaido.cultivation_server.entity.mushroom;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mushroom_reference")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MushroomReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mushroom_name_ko", length = 50)
    private String korName;
    @Column(name = "mushroom_name_en", length = 50)
    private String enName;
    @Column(name = "mushroom_scientific_name", length = 50)
    private String ScientificName;

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

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
