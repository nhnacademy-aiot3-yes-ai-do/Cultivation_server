package site.yesaido.cultivation_server.cultivation.entity.cultivation;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import site.yesaido.cultivation_server.cultivation.entity.mushroomreference.MushroomReference;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(
        name = "cultivation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cultivation_user_name",
                columnNames = {"user_id", "name"}
        )
)
public class Cultivation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "mode", length = 20)
    private CultivationMode mode = CultivationMode.GROWTH;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CultivationStatus cultivationStatus = CultivationStatus.CREATED;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mushroom_id", nullable = false)
    private MushroomReference mushroomReference;

    public void finish() {
        this.cultivationStatus = CultivationStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    // 소유권 이전
    public void changeOwner(Long newUserId) {
        this.userId = newUserId;
    }
}
