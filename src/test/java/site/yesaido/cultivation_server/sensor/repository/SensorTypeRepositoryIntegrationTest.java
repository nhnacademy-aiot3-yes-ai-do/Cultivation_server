package site.yesaido.cultivation_server.sensor.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import site.yesaido.cultivation_server.config.QuerydslConfig;
import site.yesaido.cultivation_server.sensor.entity.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(QuerydslConfig.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SensorTypeRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private EntityManager entityManager;
    @Autowired private SensorTypeRepository sensorTypeRepository;
    @Autowired private CultivationSensorRepository cultivationSensorRepository;
    @Autowired private CultivationSensorTypeRepository cultivationSensorTypeRepository;
    @Autowired private MushroomReferenceRepository mushroomReferenceRepository;
    @Autowired private MushroomReferenceThresholdRepository mushroomReferenceThresholdRepository;
    @Autowired private EnvironmentSettingRepository environmentSettingRepository;

    @Test
    void existsInUseByIdReturnsFalseWhenSensorTypeHasNoReferences() {
        SensorType sensorType = sensorTypeRepository.saveAndFlush(new SensorType("unused", "C"));

        assertFalse(sensorTypeRepository.existsInUseById(sensorType.getId()));
    }

    @Test
    void existsInUseByIdReturnsTrueWhenCultivationSensorReferencesSensorType() {
        SensorType sensorType = sensorTypeRepository.saveAndFlush(new SensorType("temperature", "C"));
        CultivationSensor sensor = cultivationSensorRepository.saveAndFlush(
                new CultivationSensor(1L, "eui-1", "model", "name", "location", "detail"));
        cultivationSensorTypeRepository.saveAndFlush(new CultivationSensorType(sensor, sensorType));

        assertTrue(sensorTypeRepository.existsInUseById(sensorType.getId()));
    }

    @Test
    void existsInUseByIdReturnsTrueWhenMushroomThresholdReferencesSensorType() {
        SensorType sensorType = sensorTypeRepository.saveAndFlush(new SensorType("humidity", "%"));
        MushroomReference mushroom = mushroomReferenceRepository.saveAndFlush(
                new MushroomReference("표고", "shiitake", "Lentinula edodes"));
        mushroomReferenceThresholdRepository.saveAndFlush(
                new MushroomReferenceThreshold(sensorType, mushroom, BigDecimal.ONE, BigDecimal.TEN));

        assertTrue(sensorTypeRepository.existsInUseById(sensorType.getId()));
    }

    @Test
    void existsInUseByIdReturnsTrueWhenEnvironmentSettingReferencesSensorType() {
        SensorType sensorType = sensorTypeRepository.saveAndFlush(new SensorType("pressure", "hPa"));
        environmentSettingRepository.saveAndFlush(new EnvironmentSetting(1L, sensorType, BigDecimal.ONE, BigDecimal.TEN));

        assertTrue(sensorTypeRepository.existsInUseById(sensorType.getId()));
    }

    @Test
    void deleteAndFlushFailsWhenSensorTypeIsReferenced() {
        SensorType sensorType = sensorTypeRepository.saveAndFlush(new SensorType("co2", "ppm"));
        environmentSettingRepository.saveAndFlush(new EnvironmentSetting(1L, sensorType, BigDecimal.ONE, BigDecimal.TEN));
        entityManager.clear();

        sensorTypeRepository.deleteById(sensorType.getId());

        assertThrows(DataIntegrityViolationException.class, sensorTypeRepository::flush);
    }
}
