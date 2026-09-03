package site.yesaido.cultivation_server.cultivation.repository.harvest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.cultivation.entity.harvest.Harvest;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface HarvestRepository extends JpaRepository<Harvest, Long> {
    Optional<Harvest> findByCultivationId(Long cultivationId);
    boolean existsByCultivationId(Long cultivationId);

    // 상대평가 모집단 (자기 자신 제외, 점수가 있는 다른 수확 건) 전체 개수
    @Query("SELECT COUNT(h) FROM Harvest h " +
            "WHERE h.productScore IS NOT NULL " +
            "AND h.id <> :harvestId " +
            "AND h.cultivation.mushroomReference.id = :mushroomId")
    long countByMushroomIdAndProductScoreIsNotNullAndIdNot(@Param("mushroomId") Long mushroomId,
                                                           @Param("harvestId") Long harvestId);

    // 자기 자신 제외, 나보다 점수가 더 높은 수확 건 개수
    @Query("SELECT COUNT(h) FROM Harvest h " +
            "WHERE h.productScore > :productScore " +
            "AND h.id <> :harvestId " +
            "AND h.cultivation.mushroomReference.id = :mushroomId")
    long countByMushroomIdAndProductScoreGreaterThanAndIdNot(@Param("mushroomId") Long mushroomId,
                                                             @Param("productScore") BigDecimal productScore,
                                                             @Param("harvestId") Long harvestId);
}
