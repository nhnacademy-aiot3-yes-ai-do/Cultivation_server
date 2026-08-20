package site.yesaido.cultivation_server.cultivation.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.*;

public interface CultivationService {
    // 경작 생성
    CultivationCreateResponse create(CultivationCreateRequest request, Long userId);

    // 경작 목록 보기
    CultivationSummaryListResponse getCultivations(Long userId);

    // 경작 상세 조회
    CultivationDetailResponse getCultivation(Long userId, Long cultivationId);

    // 경작 종료
    CultivationFinishResponse finish(Long cultivationId, Long userId);

    // 경작 이력 조회
    Page<CultivationHistoryResponse> getHistory(Long userId, Pageable pageable);

    // 경작 삭제 (OWNER만 가능)
    void delete(Long cultivationId, Long userId);
}
