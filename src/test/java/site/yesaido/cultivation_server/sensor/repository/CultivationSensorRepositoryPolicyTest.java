package site.yesaido.cultivation_server.sensor.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import site.yesaido.cultivation_server.config.QuerydslConfig;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class CultivationSensorRepositoryPolicyTest {

    @Autowired
    private CultivationSensorRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @ParameterizedTest
    @EnumSource(CultivationStatus.class)
    @DisplayName("재배지 상태와 무관하게 미삭제 센서는 사용 중이며 재사용 목록에서 제외한다")
    void undeletedSensorBlocksReuseRegardlessOfCultivationStatus(CultivationStatus status) {
        Cultivation cultivation = cultivation(1L, "source", status);
        sensor(cultivation, false);

        assertThat(repository.isDeviceEuiInUseInOtherActiveCultivation("EUI-001", -1L)).isTrue();
        assertThat(repository.findReusableSensorsForOwner(1L, -1L)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(CultivationStatus.class)
    @DisplayName("소프트삭제된 센서는 재배지 상태와 무관하게 재사용할 수 있다")
    void softDeletedSensorCanBeReusedRegardlessOfCultivationStatus(CultivationStatus status) {
        Cultivation cultivation = cultivation(1L, "source", status);
        CultivationSensor sensor = sensor(cultivation, true);

        assertThat(repository.isDeviceEuiInUseInOtherActiveCultivation("EUI-001", -1L)).isFalse();
        assertThat(repository.findReusableSensorsForOwner(1L, -1L))
                .extracting(CultivationSensor::getId).containsExactly(sensor.getId());
    }

    @Test
    @DisplayName("삭제 이력이 있어도 다른 사용자의 재배지에서 같은 EUI가 사용 중이면 재사용하지 못한다")
    void excludesDeletedHistoryWhenAnotherCultivationUsesSameEui() {
        sensor(cultivation(1L, "history", CultivationStatus.FINISHED), true);
        sensor(cultivation(2L, "current", CultivationStatus.RUNNING), false);

        assertThat(repository.findReusableSensorsForOwner(1L, -1L)).isEmpty();
        assertThat(repository.isDeviceEuiInUseInOtherActiveCultivation("EUI-001", -1L)).isTrue();
    }

    @Test
    @DisplayName("현재 재배지는 다른 재배지 검사 및 재사용 후보에서 제외한다")
    void excludesRequestedCultivation() {
        Cultivation cultivation = cultivation(1L, "current", CultivationStatus.RUNNING);
        sensor(cultivation, false);

        assertThat(repository.isDeviceEuiInUseInOtherActiveCultivation("EUI-001", cultivation.getId())).isFalse();
        assertThat(repository.findReusableSensorsForOwner(1L, cultivation.getId())).isEmpty();
    }

    private Cultivation cultivation(Long userId, String name, CultivationStatus status) {
        MushroomReference mushroom = entityManager.persist(new MushroomReference());
        return entityManager.persistAndFlush(Cultivation.builder()
                .userId(userId).name(name).cultivationStatus(status)
                .mushroomReference(mushroom).build());
    }

    private CultivationSensor sensor(Cultivation cultivation, boolean deleted) {
        CultivationSensor sensor = new CultivationSensor(cultivation.getId(), "EUI-001",
                "MODEL", "sensor", "ROOM", "shelf");
        if (deleted) {
            sensor.toDelete();
        }
        return entityManager.persistAndFlush(sensor);
    }
}
