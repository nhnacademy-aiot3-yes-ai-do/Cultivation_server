package site.yesaido.cultivation_server.entity.harvest;

import jakarta.persistence.*;
import lombok.*;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;

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

    private Double harvestWeight;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private LocalDateTime harvestedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "cultivation_id", unique = true)
    private Cultivation cultivation;
}
