package site.yesaido.cultivation_server.cultivation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.config.QuerydslConfig;
import site.yesaido.cultivation_server.cultivation.client.UserClient;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationServiceImpl;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceRepository;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, CultivationServiceImpl.class})
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CultivationDeleteIntegrationTest {

    @Autowired
    private CultivationService cultivationService;

    @Autowired
    private CultivationRepository cultivationRepository;

    @Autowired
    private MushroomReferenceRepository mushroomReferenceRepository;

    @MockitoBean
    private CultivationMemberService cultivationMemberService;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private CultivationSensorFacade cultivationSensorFacade;

    @Test
    @DisplayName("관리자 역할을 전달하는 삭제도 DELETED 상태를 DB에 반영한다")
    void deleteWithRolePersistsDeletedStatus() {
        MushroomReference mushroom = mushroomReferenceRepository.saveAndFlush(new MushroomReference());
        Cultivation cultivation = cultivationRepository.saveAndFlush(
                Cultivation.builder()
                        .userId(1L)
                        .name("삭제 대상 재배")
                        .cultivationStatus(CultivationStatus.CREATED)
                        .mushroomReference(mushroom)
                        .build()
        );

        cultivationService.delete(cultivation.getId(), 1L, null);

        Cultivation reloaded = cultivationRepository.findById(cultivation.getId()).orElseThrow();
        assertThat(reloaded.getCultivationStatus()).isEqualTo(CultivationStatus.DELETED);
        assertThat(reloaded.getDeletedAt()).isNotNull();
    }
}
