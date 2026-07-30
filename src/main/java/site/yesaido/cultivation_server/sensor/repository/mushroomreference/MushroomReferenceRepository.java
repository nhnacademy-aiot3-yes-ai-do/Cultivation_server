package site.yesaido.cultivation_server.sensor.repository.mushroomreference;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.cultivation.entity.mushroomreference.MushroomReference;

public interface MushroomReferenceRepository extends JpaRepository<MushroomReference, Long> {
}
