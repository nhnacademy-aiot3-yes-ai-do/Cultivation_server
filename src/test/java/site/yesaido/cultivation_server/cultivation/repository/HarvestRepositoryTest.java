package site.yesaido.cultivation_server.cultivation.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import site.yesaido.cultivation_server.config.QuerydslConfig;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.harvest.Harvest;
import site.yesaido.cultivation_server.cultivation.repository.harvest.HarvestRepository;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class HarvestRepositoryTest {
    @Autowired
    private HarvestRepository harvestRepository;

    @Autowired
    private MushroomReferenceRepository mushroomReferenceRepository;

    @Autowired
    private EntityManager entityManager;

    private MushroomReference oysterMushroom; // 느타리버섯
    private MushroomReference buttonMushroom; // 양송이버섯 (다른 종류)

    @BeforeEach
    void setUp() {
        oysterMushroom = mushroomReferenceRepository.save(new MushroomReference());
        buttonMushroom = mushroomReferenceRepository.save(new MushroomReference());
    }

    @Test
    @DisplayName("countByMushroomIdAndProductScoreIsNotNullAndIdNot - 같은 버섯 종류이면서 점수가 매겨진, 자기 자신을 제외한 건수만 센다")
    void countScoredExcludesSelfAndOtherMushroom() {
        Harvest target = saveHarvest(oysterMushroom, new BigDecimal("70")); // 기준 건
        saveHarvest(oysterMushroom, new BigDecimal("80")); // 같은 종류, 점수 있음 -> 포함
        saveHarvest(oysterMushroom, new BigDecimal("60")); // 같은 종류, 점수 있음 -> 포함
        saveHarvest(oysterMushroom, null); // 같은 종류, 점수 없음 -> 제외
        saveHarvest(buttonMushroom, new BigDecimal("90")); // 다른 종류 -> 제외

        long count = harvestRepository.countByMushroomIdAndProductScoreIsNotNullAndIdNot(
                oysterMushroom.getId(), target.getId());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("countByMushroomIdAndProductScoreGreaterThanAndIdNot - 같은 버섯 종류 중 자기 자신보다 점수가 높은 건수만 센다 (동점 제외)")
    void countHigherScoresExcludesTiesAndOtherMushroom() {
        Harvest target = saveHarvest(oysterMushroom, new BigDecimal("70")); // 기준 점수 70
        saveHarvest(oysterMushroom, new BigDecimal("90")); // 더 높음 -> 포함
        saveHarvest(oysterMushroom, new BigDecimal("71")); // 더 높음 -> 포함
        saveHarvest(oysterMushroom, new BigDecimal("70")); // 동점 -> 제외
        saveHarvest(oysterMushroom, new BigDecimal("50")); // 더 낮음 -> 제외
        saveHarvest(buttonMushroom, new BigDecimal("99")); // 다른 종류, 훨씬 높음 -> 제외

        long higherCount = harvestRepository.countByMushroomIdAndProductScoreGreaterThanAndIdNot(
                oysterMushroom.getId(), target.getProductScore(), target.getId());

        assertThat(higherCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("점수가 매겨진 건이 자기 자신뿐이면 두 카운트 모두 0을 반환한다")
    void countsAreZeroWhenNoOtherScoredHarvestExists() {
        Harvest target = saveHarvest(oysterMushroom, new BigDecimal("70"));

        long total = harvestRepository.countByMushroomIdAndProductScoreIsNotNullAndIdNot(
                oysterMushroom.getId(), target.getId());
        long higher = harvestRepository.countByMushroomIdAndProductScoreGreaterThanAndIdNot(
                oysterMushroom.getId(), target.getProductScore(), target.getId());

        assertThat(total).isZero();
        assertThat(higher).isZero();
    }

    private Harvest saveHarvest(MushroomReference mushroom, BigDecimal productScore) {
        Cultivation cultivation = Cultivation.builder()
                .userId(1L)
                .name("테스트 재배 " + System.nanoTime())
                .mushroomReference(mushroom)
                .build();
        entityManager.persist(cultivation);

        Harvest harvest = Harvest.builder()
                .harvestWeight(new BigDecimal("5.0"))
                .harvestedAt(LocalDateTime.now())
                .cultivation(cultivation)
                .productScore(productScore)
                .build();
        entityManager.persist(harvest);
        return harvest;
    }
}