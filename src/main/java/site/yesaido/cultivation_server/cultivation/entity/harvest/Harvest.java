package site.yesaido.cultivation_server.cultivation.entity.harvest;

import jakarta.persistence.*;
import lombok.*;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name = "harvest")
public class Harvest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal harvestWeight;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private LocalDateTime harvestedAt;

    @Column(name = "project_score")
    private BigDecimal projectScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_grade", length = 10)
    private ProductGrade productGrade;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "cultivation_id", unique = true)
    private Cultivation cultivation;
}
