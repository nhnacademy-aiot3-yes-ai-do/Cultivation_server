package site.yesaido.cultivation_server.entity.cultivationmember;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;

import java.time.LocalDateTime;

@Entity
@Table(name = "cultivation_member")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CultivationMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MemberRole role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cultivation_id", nullable = false)
    private Cultivation cultivation;

    public void updateRole(MemberRole role) {
        this.role = role;
    }
}
