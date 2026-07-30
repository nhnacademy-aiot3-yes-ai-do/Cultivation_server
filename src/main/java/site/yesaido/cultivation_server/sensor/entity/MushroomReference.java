package site.yesaido.cultivation_server.sensor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "mushroom_reference")
public class MushroomReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mushroom_name_ko")
    private String mushroomNameKo;

    @Column(name = "mushroom_name_en")
    private String mushroomNameEn;

    @Column(name = "mushroom_scientific_name", unique = true)
    private String mushroomScientificName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MushroomReference(String mushroomNameKo, String mushroomNameEn, String mushroomScientificName) {
        this.mushroomNameKo = mushroomNameKo;
        this.mushroomNameEn = mushroomNameEn;
        this.mushroomScientificName = mushroomScientificName;

        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        this.mushroomReferenceThresholds = new HashSet<>();
    }

    @OneToMany(mappedBy = "mushroomReference")
    private Set<MushroomReferenceThreshold> mushroomReferenceThresholds;

    public void isUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

}
