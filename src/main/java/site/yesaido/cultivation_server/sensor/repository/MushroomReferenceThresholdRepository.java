package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThresholdType;

import java.util.List;

public interface MushroomReferenceThresholdRepository extends JpaRepository<MushroomReferenceThreshold, Long> {
    List<MushroomReferenceThreshold> findAllByMushroomReference_idAndThresholdType(Long mushroomReferenceId, MushroomReferenceThresholdType thresholdType);
}
