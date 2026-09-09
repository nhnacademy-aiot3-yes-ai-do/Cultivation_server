package site.yesaido.cultivation_server.cultivation.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import site.yesaido.cultivation_server.config.QuerydslConfig;
import site.yesaido.cultivation_server.cultivation.client.UserClient;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.HarvestCreateRequest;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.repository.harvest.HarvestRepository;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationAccessGuard;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationServiceImpl;
import site.yesaido.cultivation_server.cultivation.service.impl.HarvestServiceImpl;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoDeleteEvent;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensorType;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.entity.SensorConnectStatus;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorTypeService;
import site.yesaido.cultivation_server.sensor.service.EnvironmentSettingPreparationService;
import site.yesaido.cultivation_server.sensor.service.EnvironmentSettingService;
import site.yesaido.cultivation_server.sensor.service.impl.CultivationSensorFacadeImpl;
import site.yesaido.cultivation_server.sensor.service.impl.CultivationSensorServiceImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({QuerydslConfig.class, CultivationServiceImpl.class, HarvestServiceImpl.class,
        CultivationSensorFacadeImpl.class, CultivationSensorServiceImpl.class,
        CultivationSensorReleaseIntegrationTest.EventConfiguration.class})
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CultivationSensorReleaseIntegrationTest {

    @Autowired private CultivationService cultivationService;
    @Autowired private HarvestService harvestService;
    @Autowired private CultivationSensorRepository sensorRepository;
    @Autowired private HarvestRepository harvestRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CommitEvents events;

    @MockitoBean private CultivationMemberService cultivationMemberService;
    @MockitoBean private UserClient userClient;
    @MockitoBean private CultivationAccessGuard cultivationAccessGuard;
    @MockitoBean private CultivationSensorTypeService cultivationSensorTypeService;
    @MockitoBean private EnvironmentSettingService environmentSettingService;
    @MockitoBean private EnvironmentSettingPreparationService environmentSettingPreparationService;

    @BeforeEach
    void resetEvents() {
        events.thresholds.clear();
        events.sensors.clear();
        events.rejectDeletion = false;
    }

    @ParameterizedTest
    @EnumSource(CloseAction.class)
    @DisplayName("종료 및 삭제는 센서를 해제하고 커밋 후 삭제 이벤트를 전달한다")
    void closesCultivationAndReleasesSensors(CloseAction action) {
        Fixture fixture = createFixture(true);

        assertThat(sensorRepository.isDeviceEuiInUseInOtherActiveCultivation(fixture.deviceEui(), -1L)).isTrue();
        close(action, fixture.cultivationId());

        Cultivation cultivation = entityManager.find(Cultivation.class, fixture.cultivationId());
        CultivationSensor sensor = sensorRepository.findById(fixture.sensorId()).orElseThrow();
        assertThat(cultivation.getCultivationStatus()).isEqualTo(action.expectedStatus());
        assertThat(sensor.isDeleted()).isTrue();
        assertThat(sensor.getSensorStatus()).isEqualTo(SensorConnectStatus.OFFLINE);
        assertThat(sensorRepository.isDeviceEuiInUseInOtherActiveCultivation(fixture.deviceEui(), -1L)).isFalse();
        assertThat(entityManager.find(EnvironmentSetting.class, fixture.environmentSettingId())).isNotNull();
        assertThat(events.thresholds).singleElement().satisfies(event -> {
            assertThat(event.cultivationId()).isEqualTo(fixture.cultivationId());
            assertThat(event.sensorRangeList()).isEmpty();
        });
        assertThat(events.sensors).singleElement().satisfies(event -> {
            assertThat(event.cultivationId()).isEqualTo(fixture.cultivationId());
            assertThat(event.deviceEui()).isEqualTo(fixture.deviceEui());
            assertThat(event.sensorType()).isEqualTo("TEMPERATURE");
        });
        assertThat(harvestRepository.existsByCultivationId(fixture.cultivationId()))
                .isEqualTo(action == CloseAction.HARVEST);
    }

    @ParameterizedTest
    @EnumSource(CloseAction.class)
    @DisplayName("센서가 없는 재배지도 종료 및 삭제할 수 있다")
    void closesCultivationWithoutSensors(CloseAction action) {
        Fixture fixture = createFixture(false);

        close(action, fixture.cultivationId());

        assertThat(entityManager.find(Cultivation.class, fixture.cultivationId()).getCultivationStatus())
                .isEqualTo(action.expectedStatus());
        assertThat(events.sensors).isEmpty();
        assertThat(events.thresholds).singleElement().satisfies(event ->
                assertThat(event.sensorRangeList()).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(CloseAction.class)
    @DisplayName("센서 정리 중 실패하면 재배지와 센서 변경 및 커밋 후 이벤트를 모두 롤백한다")
    void rollsBackCultivationAndSensorsWhenCleanupFails(CloseAction action) {
        Fixture fixture = createFixture(true);
        events.rejectDeletion = true;

        assertThatThrownBy(() -> close(action, fixture.cultivationId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("sensor cleanup failed");

        Cultivation cultivation = entityManager.find(Cultivation.class, fixture.cultivationId());
        CultivationSensor sensor = sensorRepository.findById(fixture.sensorId()).orElseThrow();
        assertThat(cultivation.getCultivationStatus()).isEqualTo(CultivationStatus.RUNNING);
        assertThat(cultivation.getFinishedAt()).isNull();
        assertThat(cultivation.getDeletedAt()).isNull();
        assertThat(sensor.isDeleted()).isFalse();
        assertThat(sensor.getSensorStatus()).isEqualTo(SensorConnectStatus.ONLINE);
        assertThat(harvestRepository.existsByCultivationId(fixture.cultivationId())).isFalse();
        assertThat(events.thresholds).isEmpty();
        assertThat(events.sensors).isEmpty();
    }

    private void close(CloseAction action, long cultivationId) {
        switch (action) {
            case FINISH -> cultivationService.finish(cultivationId, 1L);
            case DELETE -> cultivationService.delete(cultivationId, 999L, "ADMIN");
            case DELETE_WITHOUT_ROLE -> cultivationService.deleteWithoutRole(cultivationId, 1L);
            case HARVEST -> harvestService.createHarvest(cultivationId, 1L,
                    new HarvestCreateRequest(BigDecimal.ONE, "release test"));
        }
    }

    private Fixture createFixture(boolean withSensor) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString();
            MushroomReference mushroom = new MushroomReference();
            entityManager.persist(mushroom);
            Cultivation cultivation = Cultivation.builder()
                    .userId(1L).name("release-" + suffix)
                    .cultivationStatus(CultivationStatus.RUNNING).mode(CultivationMode.HARVEST)
                    .mushroomReference(mushroom).build();
            entityManager.persist(cultivation);
            SensorType type = new SensorType("TEMPERATURE", "test-" + suffix);
            entityManager.persist(type);
            EnvironmentSetting setting = new EnvironmentSetting(cultivation.getId(), type,
                    BigDecimal.TEN, BigDecimal.valueOf(20));
            entityManager.persist(setting);
            if (!withSensor) {
                return new Fixture(cultivation.getId(), null, setting.getId(), null);
            }
            CultivationSensor sensor = new CultivationSensor(cultivation.getId(), "EUI-" + suffix,
                    "MODEL", "sensor", "ROOM", "shelf");
            ReflectionTestUtils.setField(sensor, "sensorStatus", SensorConnectStatus.ONLINE);
            entityManager.persist(sensor);
            entityManager.persist(new CultivationSensorType(sensor, type));
            CultivationSensor deletedSensor = new CultivationSensor(cultivation.getId(), "OLD-" + suffix,
                    "MODEL", "old sensor", "ROOM", "shelf");
            deletedSensor.toDelete();
            entityManager.persist(deletedSensor);
            entityManager.persist(new CultivationSensorType(deletedSensor, type));
            return new Fixture(cultivation.getId(), sensor.getId(), setting.getId(), sensor.getDeviceEui());
        });
    }

    enum CloseAction {
        FINISH, DELETE, DELETE_WITHOUT_ROLE, HARVEST;

        CultivationStatus expectedStatus() {
            return this == FINISH || this == HARVEST
                    ? CultivationStatus.FINISHED : CultivationStatus.DELETED;
        }
    }

    record Fixture(long cultivationId, Long sensorId, long environmentSettingId, String deviceEui) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class EventConfiguration {
        @Bean
        CommitEvents commitEvents() {
            return new CommitEvents();
        }
    }

    static class CommitEvents {
        final List<ThresholdInfoEvent> thresholds = new ArrayList<>();
        final List<SensorInfoDeleteEvent> sensors = new ArrayList<>();
        boolean rejectDeletion;

        @EventListener
        public void rejectDeletionWhenRequested(SensorInfoDeleteEvent event) {
            if (rejectDeletion) {
                throw new IllegalStateException("sensor cleanup failed");
            }
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void thresholdCommitted(ThresholdInfoEvent event) {
            thresholds.add(event);
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void sensorCommitted(SensorInfoDeleteEvent event) {
            sensors.add(event);
        }
    }
}
