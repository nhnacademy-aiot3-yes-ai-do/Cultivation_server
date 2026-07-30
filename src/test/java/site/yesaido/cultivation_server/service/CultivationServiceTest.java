package site.yesaido.cultivation_server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.*;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAlreadyExist;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.exception.MushroomNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.mushroomreference.MushroomReferenceRepository;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationMemberServiceImpl;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationServiceImpl;

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

    @InjectMocks
    private CultivationServiceImpl service;

    @Test
    @DisplayName("경작 생성 성공")
    void createCultivationSuccess() {
        Long userId = 1L;
        Long mushroomId = 100L;
        CultivationCreateRequest request = new CultivationCreateRequest("테스트 버섯", mushroomId, Collections.emptyList());

        MushroomReference mushroom = MushroomReference.builder().build();

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
        MushroomReference mushroom = MushroomReference.builder().build();
        Cultivation cultivation1 = Cultivation.builder().name("농장1").mushroomReference(mushroom).build();
        Cultivation cultivation2 = Cultivation.builder().name("농장2").mushroomReference(mushroom).build();

        when(cultivationRepository.findAllByMemberUserId(userId)).thenReturn(List.of(cultivation1, cultivation2));

        List<CultivationSummaryResponse> result = service.getCultivations(userId);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().name()).isEqualTo(cultivation1.getName());
        assertThat(result.get(1).name()).isEqualTo(cultivation2.getName());
    }

    @Test
    @DisplayName("단일 경작 상세 조회 성공")
    void getCultivationDetail_Success() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MushroomReference mushroom = MushroomReference.builder().build();
        Cultivation cultivation = Cultivation.builder()
                .name("경작 상세")
                .mushroomReference(mushroom)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        when(cultivationRepository.isMember(cultivationId, userId)).thenReturn(true);

        CultivationDetailResponse response = service.getCultivation(userId, cultivationId);
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(cultivation.getName());
    }

    @Test
    @DisplayName("단일 경작 조회 실패 - 권한이 없는 유저의 접근")
    void getCultivationDetailFailAccessDenied() {
        Long cultivationId = 1L;
        Long unauthorizedUserId = 100L;
        Cultivation cultivation = Cultivation.builder().name("남의 농장").build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        when(cultivationRepository.isMember(cultivationId, unauthorizedUserId)).thenReturn(false);

        assertThatThrownBy(() -> service.getCultivation(unauthorizedUserId, cultivationId)).isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("단일 경작 조회 실패 - 없는 경작 ID 조회")
    void getCultivationDetailFailNotFound() {
        Long invalidCultivationId = 100L;
        Long userId = 1L;

        when(cultivationRepository.findById(invalidCultivationId)).thenReturn(Optional.empty());
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
}
