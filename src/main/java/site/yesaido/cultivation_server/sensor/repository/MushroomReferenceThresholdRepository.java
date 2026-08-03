package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;

public interface MushroomReferenceThresholdRepository extends JpaRepository<MushroomReferenceThreshold, Long> {
}
