package site.yesaido.cultivation_server.cultivation.entity.mushroomreference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mushroom_reference")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MushroomReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
