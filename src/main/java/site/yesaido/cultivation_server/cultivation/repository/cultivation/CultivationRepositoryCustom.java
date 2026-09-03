package site.yesaido.cultivation_server.cultivation.repository.cultivation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationHistoryResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.sensor.dto.projection.CultivationSummaryProjection;

import java.util.List;
import java.util.Optional;

public interface CultivationRepositoryCustom {
    List<Cultivation> findAllByMemberUserId(Long userId);

    // 이력 조회용
    Page<CultivationHistoryResponse> findHistoryByMemberUserId(Long userId, Pageable pageable);

    List<CultivationSummaryProjection> findSummaryProjectionsByMemberUserId(Long userId);

    Optional<CultivationSummaryProjection> findDetailProjectionByUserIdAndCultivationId(Long userId, Long cultivationId);
}
