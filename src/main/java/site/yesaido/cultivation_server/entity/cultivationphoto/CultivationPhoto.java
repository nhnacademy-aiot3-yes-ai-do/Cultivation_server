package site.yesaido.cultivation_server.entity.cultivationphoto;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cultivation_photo")
public class CultivationPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "storage_type", nullable = false, length = 50)
    @Builder.Default
    private StorageType storageType = StorageType.MINIO;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cultivation_id", nullable = false)
    private Cultivation cultivation;
}
