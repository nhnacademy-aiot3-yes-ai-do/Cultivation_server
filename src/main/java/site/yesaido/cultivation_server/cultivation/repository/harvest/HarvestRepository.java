package site.yesaido.cultivation_server.cultivation.repository.harvest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.cultivation.entity.harvest.Harvest;

import java.util.Optional;

@Repository
public interface HarvestRepository extends JpaRepository<Harvest, Long> {
    Optional<Harvest> findByCultivationId(Long cultivationId);
    boolean existsByCultivationId(Long cultivationId);
}
