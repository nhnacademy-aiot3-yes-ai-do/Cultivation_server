package site.yesaido.cultivation_server.cultivation.repository.cultivation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationHistoryResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;

import java.util.List;

public interface CultivationRepositoryCustom {
    List<Cultivation> findAllByMemberUserId(Long userId);

    // 이력 조회용
    Page<CultivationHistoryResponse> findHistoryByMemberUserId(Long userId, Pageable pageable);
}
