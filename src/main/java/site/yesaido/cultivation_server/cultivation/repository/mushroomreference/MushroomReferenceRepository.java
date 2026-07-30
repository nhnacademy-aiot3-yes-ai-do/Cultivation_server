package site.yesaido.cultivation_server.cultivation.repository.mushroomreference;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.cultivation.entity.mushroomreference.MushroomReference;

@Repository
public interface MushroomReferenceRepository extends CrudRepository<MushroomReference, Long> {
}
