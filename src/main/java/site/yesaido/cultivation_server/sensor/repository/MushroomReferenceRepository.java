package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;

import java.util.List;

public interface MushroomReferenceRepository extends JpaRepository<MushroomReference, Long> {
    boolean existsMushroomReferenceByMushroomScientificName(String mushroomScientificName);

    boolean existsMushroomReferenceById(Long id);

    @Query("""
        SELECT mr
        FROM MushroomReferenceThreshold mrt
        JOIN FETCH MushroomReference mr ON mrt.mushroomReference = mr
        JOIN FETCH SensorType st ON mrt.sensorType = st
        WHERE mr.id = ?1
    """)
    MushroomReference findMushroomReferenceById(Long id);

    @Query("""
        SELECT mr
        FROM MushroomReferenceThreshold mrt
        JOIN FETCH MushroomReference mr ON mrt.mushroomReference = mr
        JOIN FETCH SensorType st ON mrt.sensorType = st
    """)
    List<MushroomReference> findAllMushroomReference();
}
