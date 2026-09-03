package site.yesaido.cultivation_server.cultivation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.HarvestCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.ProductScoreUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestCreateResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestDetailResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.ProductScoreUpdateResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.harvest.Harvest;
import site.yesaido.cultivation_server.cultivation.entity.harvest.ProductGrade;
import site.yesaido.cultivation_server.cultivation.exception.*;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.harvest.HarvestRepository;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationAccessGuard;
import site.yesaido.cultivation_server.cultivation.service.impl.HarvestServiceImpl;
import site.yesaido.cultivation_server.rabbitmq.event.HarvestCompletedEvent;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class HarvestServiceTest {
    @Mock
    private HarvestRepository harvestRepository;

    @Mock
    private CultivationRepository cultivationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CultivationMemberService cultivationMemberService;

    @Mock
    private CultivationSensorFacade cultivationSensorFacade;

    @Mock
    private CultivationAccessGuard cultivationAccessGuard;

    @InjectMocks
    private HarvestServiceImpl harvestService;

    @Test
    @DisplayName("수확 기록 성공 - 재배가 자동으로 종료 처리됨.")
    void createHarvestSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;
        HarvestCreateRequest request = new HarvestCreateRequest(new BigDecimal("3.5"), "메모");

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .mode(CultivationMode.HARVEST)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        when(harvestRepository.existsByCultivationId(cultivationId)).thenReturn(false);

        HarvestCreateResponse response = harvestService.createHarvest(cultivationId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.harvestWeight()).isEqualTo(request.harvestWeight());
        assertThat(response.productScore()).isNull();
        assertThat(cultivation.getCultivationStatus()).isEqualTo(CultivationStatus.FINISHED);
        verify(harvestRepository, times(1)).save(any(Harvest.class));
        verify(eventPublisher).publishEvent(any(HarvestCompletedEvent.class));
        verify(cultivationSensorFacade).deleteAll(userId, cultivationId);
    }

    @Test
    @DisplayName("수확 기록 실패 - 존재하지 않는 재배")
    void createHarvestFailNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;
        HarvestCreateRequest request = new HarvestCreateRequest(new BigDecimal("3.5"), null);

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> harvestService.createHarvest(cultivationId, userId, request))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("수확 기록 실패 - 재배 소유자가 아닌 경우")
    void createHarvestFailAccessDenied() {
        Long ownerId = 1L;
        Long otherUserId = 2L;
        Long cultivationId = 100L;
        HarvestCreateRequest request = new HarvestCreateRequest(new BigDecimal("3.5"), null);

        Cultivation cultivation = Cultivation.builder()
                .userId(ownerId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .mode(CultivationMode.HARVEST)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        doThrow(new CultivationAccessDeniedException(cultivationId))
                .when(cultivationMemberService).verifyManagerAccess(cultivationId, otherUserId);

        assertThatThrownBy(() -> harvestService.createHarvest(cultivationId, otherUserId, request))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("수확 기록 실패 - 이미 종료된 재배")
    void createHarvestFailAlreadyFinished() {
        Long userId = 1L;
        Long cultivationId = 100L;
        HarvestCreateRequest request = new HarvestCreateRequest(new BigDecimal("3.5"), null);

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.FINISHED)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        assertThatThrownBy(() -> harvestService.createHarvest(cultivationId, userId, request))
                .isInstanceOf(CultivationAlreadyFinishedException.class);
    }

    @Test
    @DisplayName("수확 기록 실패 - 이미 수확이 기록된 재배")
    void createHarvestFailAlreadyExist() {
        Long userId = 1L;
        Long cultivationId = 100L;
        HarvestCreateRequest request = new HarvestCreateRequest(new BigDecimal("3.5"), null);

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .mode(CultivationMode.HARVEST)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        when(harvestRepository.existsByCultivationId(cultivationId)).thenReturn(true);

        assertThatThrownBy(() -> harvestService.createHarvest(cultivationId, userId, request))
                .isInstanceOf(HarvestAlreadyExistException.class);
    }

    @Test
    @DisplayName("수확 상세 조회 성공")
    void getHarvestSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.FINISHED)
                .build();

        Harvest harvest = Harvest.builder()
                .harvestWeight(new BigDecimal("5.0"))
                .memo("메모")
                .harvestedAt(LocalDateTime.now())
                .cultivation(cultivation)
                .build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        when(harvestRepository.findByCultivationId(cultivationId)).thenReturn(Optional.of(harvest));

        HarvestDetailResponse response = harvestService.getHarvest(cultivationId, userId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(cultivation.getName());
        assertThat(response.harvestWeight()).isEqualTo(harvest.getHarvestWeight());
    }

    @Test
    @DisplayName("수확 상세 조회 실패 - 재배 멤버가 아닌 경우")
    void getHarvestFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;

        when(cultivationAccessGuard.requireMember(cultivationId, userId))
                .thenThrow(new CultivationAccessDeniedException(cultivationId));

        assertThatThrownBy(() -> harvestService.getHarvest(cultivationId, userId))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("수확 상세 조회 실패 - 수확 기록이 없는 경우")
    void getHarvestFailNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder().name("버섯 농장").build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        when(harvestRepository.findByCultivationId(cultivationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> harvestService.getHarvest(cultivationId, userId))
                .isInstanceOf(HarvestNotFoundException.class);
    }

    @Test
    @DisplayName("상품 점수 업데이트 성공 - 같은 버섯 종류 내 상위 5% 이내면 TOP 등급으로 분류")
    void updateProductScoreSuccessTopGrade() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long mushroomId = 1L;
        ProductScoreUpdateRequest request = new ProductScoreUpdateRequest(new BigDecimal("95"));

        MushroomReference mushroomReference = mock(MushroomReference.class);
        when(mushroomReference.getId()).thenReturn(mushroomId);

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .mushroomReference(mushroomReference)
                .build();
        Harvest harvest = Harvest.builder()
                .harvestWeight(new BigDecimal("5.0"))
                .harvestedAt(LocalDateTime.now())
                .cultivation(cultivation)
                .build();

        when(cultivationRepository.existsById(cultivationId)).thenReturn(true);
        when(harvestRepository.findByCultivationId(cultivationId)).thenReturn(Optional.of(harvest));
        when(harvestRepository.countByMushroomIdAndProductScoreIsNotNullAndIdNot(eq(mushroomId), any()))
                .thenReturn(19L);
        when(harvestRepository.countByMushroomIdAndProductScoreGreaterThanAndIdNot(eq(mushroomId), any(), any()))
                .thenReturn(0L);

        ProductScoreUpdateResponse response = harvestService.updateProductScore(cultivationId, userId, request);

        assertThat(response.productScore()).isEqualTo(request.productScore());
        assertThat(response.productGrade()).isEqualTo(ProductGrade.TOP);
    }

    @Test
    @DisplayName("상품 점수 업데이트 실패 - 재배 소유자가 아닌 경우")
    void updateProductScoreFailAccessDenied() {
        Long otherUserId = 2L;
        Long cultivationId = 100L;
        ProductScoreUpdateRequest request = new ProductScoreUpdateRequest(new BigDecimal("95"));

        when(cultivationRepository.existsById(cultivationId)).thenReturn(true);
        doThrow(new CultivationAccessDeniedException(cultivationId))
                .when(cultivationMemberService).verifyManagerAccess(cultivationId, otherUserId);

        assertThatThrownBy(() -> harvestService.updateProductScore(cultivationId, otherUserId, request))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("상품 점수 업데이트 실패 - 수확 기록이 없는 경우")
    void updateProductScoreFailHarvestNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;
        ProductScoreUpdateRequest request = new ProductScoreUpdateRequest(new BigDecimal("95"));

        when(cultivationRepository.existsById(cultivationId)).thenReturn(true);
        when(harvestRepository.findByCultivationId(cultivationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> harvestService.updateProductScore(cultivationId, userId, request))
                .isInstanceOf(HarvestNotFoundException.class);
    }

    @Test
    @DisplayName("수확 기록 실패 - 아직 수확 모드로 전환되지 않은 경우")
    void createHarvestFailNotInHarvestMode() {
        Long userId = 1L;
        Long cultivationId = 100L;
        HarvestCreateRequest request = new HarvestCreateRequest(new BigDecimal("3.5"), null);

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .build(); // mode 기본값 GROWTH

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        assertThatThrownBy(() -> harvestService.createHarvest(cultivationId, userId, request))
                .isInstanceOf(CultivationNotInHarvestModeException.class);

        verify(harvestRepository, never()).existsByCultivationId(cultivationId);
    }

    @Test
    @DisplayName("수확 기록 실패 - 삭제된 재배")
    void createHarvestFailDeleted() {
        Long userId = 1L;
        Long cultivationId = 100L;
        HarvestCreateRequest request = new HarvestCreateRequest(new BigDecimal("3.5"), null);

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.DELETED)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        assertThatThrownBy(() -> harvestService.createHarvest(cultivationId, userId, request))
                .isInstanceOf(CultivationAlreadyDeletedException.class);

        verify(harvestRepository, never()).existsByCultivationId(cultivationId);
    }

    @Test
    @DisplayName("상품 점수 업데이트(internal) 성공 - 매니저 권한 검증 없이 처리된다")
    void updateProductScoreInternalSuccess() {
        Long cultivationId = 100L;
        Long mushroomId = 1L;
        ProductScoreUpdateRequest request = new ProductScoreUpdateRequest(new BigDecimal("80"));

        MushroomReference mushroomReference = mock(MushroomReference.class);
        when(mushroomReference.getId()).thenReturn(mushroomId);

        Cultivation cultivation = Cultivation.builder()
                .name("버섯 농장")
                .mushroomReference(mushroomReference)
                .build();
        Harvest harvest = Harvest.builder()
                .harvestWeight(new BigDecimal("5.0"))
                .harvestedAt(LocalDateTime.now())
                .cultivation(cultivation)
                .build();

        when(cultivationRepository.existsById(cultivationId)).thenReturn(true);
        when(harvestRepository.findByCultivationId(cultivationId)).thenReturn(Optional.of(harvest));
        when(harvestRepository.countByMushroomIdAndProductScoreIsNotNullAndIdNot(eq(mushroomId), any()))
                .thenReturn(9L);
        when(harvestRepository.countByMushroomIdAndProductScoreGreaterThanAndIdNot(eq(mushroomId), any(), any()))
                .thenReturn(1L);

        ProductScoreUpdateResponse response = harvestService.updateProductScoreInternal(cultivationId, request);

        assertThat(response.productScore()).isEqualTo(request.productScore());
        assertThat(response.productGrade()).isEqualTo(ProductGrade.HIGH);
        verify(cultivationMemberService, never()).verifyManagerAccess(anyLong(), anyLong());
    }

    @Test
    @DisplayName("상품 점수 업데이트(internal) 실패 - 존재하지 않는 재배")
    void updateProductScoreInternalFailCultivationNotFound() {
        Long cultivationId = 100L;
        ProductScoreUpdateRequest request = new ProductScoreUpdateRequest(new BigDecimal("80"));

        when(cultivationRepository.existsById(cultivationId)).thenReturn(false);

        assertThatThrownBy(() -> harvestService.updateProductScoreInternal(cultivationId, request))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("상품 점수 업데이트(internal) 실패 - 수확 기록이 없는 경우")
    void updateProductScoreInternalFailHarvestNotFound() {
        Long cultivationId = 100L;
        ProductScoreUpdateRequest request = new ProductScoreUpdateRequest(new BigDecimal("80"));

        when(cultivationRepository.existsById(cultivationId)).thenReturn(true);
        when(harvestRepository.findByCultivationId(cultivationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> harvestService.updateProductScoreInternal(cultivationId, request))
                .isInstanceOf(HarvestNotFoundException.class);
    }
}
