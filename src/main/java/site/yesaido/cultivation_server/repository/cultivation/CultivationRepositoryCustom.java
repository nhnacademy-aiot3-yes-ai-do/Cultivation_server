package site.yesaido.cultivation_server.repository.cultivation;

import site.yesaido.cultivation_server.entity.cultivation.Cultivation;

import java.util.List;

public interface CultivationRepositoryCustom {
    List<Cultivation> findAllByMemberUserId(Long userId);
}
