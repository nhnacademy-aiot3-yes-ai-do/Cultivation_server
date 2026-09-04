package site.yesaido.cultivation_server.cultivation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import site.yesaido.cultivation_server.cultivation.client.UserClient;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.*;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.cultivation.exception.*;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.cultivationmember.CultivationMemberRepository;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationMemberServiceImpl;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationServiceImpl;
import site.yesaido.cultivation_server.sensor.dto.projection.CultivationSummaryProjection;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CultivationServiceTest {
    @Mock
    private CultivationRepository cultivationRepository;

    @Mock
    private MushroomReferenceRepository mushroomReferenceRepository;

    @Mock
    private CultivationMemberServiceImpl cultivationMemberService;

    @Mock
    private CultivationMemberRepository cultivationMemberRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private CultivationServiceImpl service;

    @Test
    @DisplayName("경작 생성 성공")
    void createCultivationSuccess() {
        Long userId = 1L;
        Long mushroomId = 100L;
        CultivationCreateRequest request = new CultivationCreateRequest("테스트 버섯", mushroomId, Collections.emptyList());

        MushroomReference mushroom = new MushroomReference();

        when(cultivationRepository.existsByUserIdAndName(userId, request.name())).thenReturn(false);

        when(mushroomReferenceRepository.findById(mushroomId)).thenReturn(Optional.of(mushroom));

        CultivationCreateResponse response = service.create(request, userId);

        assertThat(response).isNotNull();
        verify(cultivationRepository, times(1)).save(any(Cultivation.class));
    }

    @Test
    @DisplayName("경작 생성 실패 - 유저가 같은 이름의 경작을 이미 가지고 있는 경우")
    void createCultivationFail() {
        Long userId = 1L;
        CultivationCreateRequest request = new CultivationCreateRequest("테스트 버섯", 100L, Collections.emptyList());

        when(cultivationRepository.existsByUserIdAndName(userId, request.name())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request, userId)).isInstanceOf(CultivationAlreadyExist.class);
        verify(cultivationRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("경작 생성 실패 - 존재하지 않는 버섯 종류 선택")
    void createFailMushroomNotFound() {
        Long userId = 1L;
        Long invalidMushroomId = 999L;
        CultivationCreateRequest request = new CultivationCreateRequest("테스트 버섯", invalidMushroomId, Collections.emptyList());

        when(cultivationRepository.existsByUserIdAndName(userId, request.name())).thenReturn(false);
        when(mushroomReferenceRepository.findById(invalidMushroomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, userId)).isInstanceOf(MushroomNotFoundException.class);
    }

    @Test
    @DisplayName("경작 목록 조회 성공")
    void getCultivationsSuccess() {
        Long userId = 1L;
        MushroomReference mushroom = new MushroomReference();
        Cultivation cultivation1 = Cultivation.builder().name("농장1").mushroomReference(mushroom).build();
        Cultivation cultivation2 = Cultivation.builder().name("농장2").mushroomReference(mushroom).build();

        CultivationSummaryProjection projection1 = mock(CultivationSummaryProjection.class);
        CultivationSummaryProjection projection2 = mock(CultivationSummaryProjection.class);
        when(projection1.cultivationId()).thenReturn(1L);
        when(projection1.name()).thenReturn("농장1");
        when(projection1.memberCount()).thenReturn(1L);
        when(projection2.cultivationId()).thenReturn(2L);
        when(projection2.name()).thenReturn("농장2");
        when(projection2.memberCount()).thenReturn(1L);
        when(cultivationRepository.findSummaryProjectionsByMemberUserId(userId))
                .thenReturn(List.of(projection1, projection2));

        CultivationSummaryListResponse result = service.getCultivations(userId);

        assertThat(result.cultivationSummaryResponses()).hasSize(2);
        assertThat(result.cultivationSummaryResponses().getFirst().name()).isEqualTo(cultivation1.getName());
        assertThat(result.cultivationSummaryResponses().get(1).name()).isEqualTo(cultivation2.getName());
    }

    @Test
    @DisplayName("단일 경작 상세 조회 성공")
    void getCultivationDetail_Success() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MushroomReference mushroom = new MushroomReference();
        Cultivation cultivation = Cultivation.builder()
                .name("경작 상세")
                .mushroomReference(mushroom)
                .build();

        CultivationSummaryProjection projection = mock(CultivationSummaryProjection.class);
        when(projection.cultivationId()).thenReturn(cultivationId);
        when(projection.name()).thenReturn(cultivation.getName());
        when(projection.myRole()).thenReturn(MemberRole.OWNER);
        when(cultivationRepository.findDetailProjectionByUserIdAndCultivationId(userId, cultivationId))
                .thenReturn(Optional.of(projection));

        CultivationDetailResponse response = service.getCultivation(userId, cultivationId);
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(cultivation.getName());
        assertThat(response.myRole()).isEqualTo(MemberRole.OWNER);
    }

    @Test
    @DisplayName("단일 경작 상세 조회 성공 - 관리자는 멤버가 아니어도 조회 가능하고 myRole은 null")
    void getCultivationDetailSuccessForAdmin() {
        Long adminId = 999L;
        Long cultivationId = 100L;
        CultivationSummaryProjection projection = mock(CultivationSummaryProjection.class);
        when(projection.myRole()).thenReturn(MemberRole.MEMBER);
        when(cultivationRepository.findDetailProjectionByUserIdAndCultivationId(adminId, cultivationId))
                .thenReturn(Optional.of(projection));

        CultivationDetailResponse response = service.getCultivation(adminId, cultivationId);

        assertThat(response).isNotNull();
        assertThat(response.myRole()).isEqualTo(MemberRole.MEMBER);
        verify(cultivationRepository).findDetailProjectionByUserIdAndCultivationId(adminId, cultivationId);
    }

    @Test
    @DisplayName("단일 경작 조회 실패 - 권한이 없는 유저의 접근")
    void getCultivationDetailFailAccessDenied() {
        Long cultivationId = 1L;
        Long unauthorizedUserId = 100L;

        CultivationSummaryProjection projection = mock(CultivationSummaryProjection.class);
        when(cultivationRepository.findDetailProjectionByUserIdAndCultivationId(unauthorizedUserId, cultivationId))
                .thenReturn(Optional.of(projection));

        assertThatThrownBy(() -> service.getCultivation(unauthorizedUserId, cultivationId)).isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("단일 경작 조회 실패 - 없는 경작 ID 조회")
    void getCultivationDetailFailNotFound() {
        Long invalidCultivationId = 100L;
        Long userId = 1L;

        when(cultivationRepository.findDetailProjectionByUserIdAndCultivationId(userId, invalidCultivationId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCultivation(userId, invalidCultivationId)).isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("재배 종료 성공 - 상태가 FINISHED로 변경됨")
    void finishSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.CREATED)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        CultivationFinishResponse response = service.finish(cultivationId, userId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(CultivationStatus.FINISHED);
        verify(cultivationMemberService).verifyOwnerAccess(cultivationId, userId);
    }

    @Test
    @DisplayName("재배 이력 조회 성공 - 페이징 결과가 반환됨")
    void getHistorySuccess() {
        Long userId = 1L;
        PageRequest pageRequest = PageRequest.of(0, 20);

        Page<CultivationHistoryResponse> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);
        when(cultivationRepository.findHistoryByMemberUserId(userId, pageRequest)).thenReturn(emptyPage);

        Page<CultivationHistoryResponse> result = service.getHistory(userId, pageRequest);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(emptyPage.getTotalElements());
    }

    @Test
    @DisplayName("재배 종료 실패 - Owner가 아닌 경우")
    void finishFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.CREATED)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        doThrow(new CultivationAccessDeniedException(cultivationId))
                .when(cultivationMemberService).verifyOwnerAccess(cultivationId, userId);

        assertThatThrownBy(() -> service.finish(cultivationId, userId))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("경작 삭제 성공 - 상태가 DELETED로 변경됨")
    void deleteSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        service.deleteWithoutRole(cultivationId, userId);

        assertThat(cultivation.getCultivationStatus()).isEqualTo(CultivationStatus.DELETED);
        verify(cultivationMemberService).verifyOwnerAccess(cultivationId, userId, null);
    }

    @Test
    @DisplayName("경작 삭제 성공 - 관리자는 소유자가 아니어도 삭제 가능")
    void deleteSuccessForAdmin() {
        Long adminId = 999L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        service.delete(cultivationId, adminId, "ADMIN");

        assertThat(cultivation.getCultivationStatus()).isEqualTo(CultivationStatus.DELETED);
        verify(cultivationMemberService).verifyOwnerAccess(cultivationId, adminId, "ADMIN");
    }

    @Test
    @DisplayName("경작 삭제 실패 - Owner가 아닌 경우")
    void deleteFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        doThrow(new CultivationAccessDeniedException(cultivationId))
                .when(cultivationMemberService).verifyOwnerAccess(cultivationId, userId, null);

        assertThatThrownBy(() -> service.delete(cultivationId, userId, null))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("경작 삭제 실패 - 이미 삭제된 경작")
    void deleteFailAlreadyDeleted() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.DELETED)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        assertThatThrownBy(() -> service.deleteWithoutRole(cultivationId, userId))
                .isInstanceOf(CultivationAlreadyDeletedException.class);
    }

    @Test
    @DisplayName("경작 삭제 실패 - 없는 경작 ID")
    void deleteFailNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteWithoutRole(cultivationId, userId))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("수확 모드 전환 성공 - GROWTH에서 HARVEST로 변경됨")
    void switchToHarvestModeSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        CultivationModeChangeResponse response = service.switchToHarvestMode(cultivationId, userId);

        assertThat(response).isNotNull();
        assertThat(response.mode()).isEqualTo(CultivationMode.HARVEST);
        assertThat(cultivation.getMode()).isEqualTo(CultivationMode.HARVEST);
        verify(cultivationMemberService).verifyManagerAccess(cultivationId, userId);
    }

    @Test
    @DisplayName("수확 모드 전환 실패 - 없는 경작 ID")
    void switchToHarvestModeFailNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.switchToHarvestMode(cultivationId, userId))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("수확 모드 전환 실패 - 이미 종료된 재배")
    void switchToHarvestModeFailAlreadyFinished() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.FINISHED)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        assertThatThrownBy(() -> service.switchToHarvestMode(cultivationId, userId))
                .isInstanceOf(CultivationAlreadyFinishedException.class);
    }

    @Test
    @DisplayName("수확 모드 전환 실패 - 이미 수확 모드인 경우")
    void switchToHarvestModeFailAlreadyInHarvestMode() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .mode(CultivationMode.HARVEST)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        assertThatThrownBy(() -> service.switchToHarvestMode(cultivationId, userId))
                .isInstanceOf(CultivationAlreadyInHarvestModeException.class);
    }

    @Test
    @DisplayName("수확 모드 전환 실패 - MANAGER 이상 권한이 없는 경우")
    void switchToHarvestModeFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.RUNNING)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        doThrow(new CultivationAccessDeniedException(cultivationId))
                .when(cultivationMemberService).verifyManagerAccess(cultivationId, userId);

        assertThatThrownBy(() -> service.switchToHarvestMode(cultivationId, userId))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("수확 모드 전환 실패 - 삭제된 재배")
    void switchToHarvestModeFailDeleted() {
        Long userId = 1L;
        Long cultivationId = 100L;

        Cultivation cultivation = Cultivation.builder()
                .userId(userId)
                .name("버섯 농장")
                .cultivationStatus(CultivationStatus.DELETED)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        assertThatThrownBy(() -> service.switchToHarvestMode(cultivationId, userId))
                .isInstanceOf(CultivationAlreadyDeletedException.class);
    }
}
