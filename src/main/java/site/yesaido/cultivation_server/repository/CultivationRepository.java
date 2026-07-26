package site.yesaido.cultivation_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;

@Repository
public interface CultivationRepository extends JpaRepository<Cultivation, Long> {
    boolean existsByName(String name);
}
