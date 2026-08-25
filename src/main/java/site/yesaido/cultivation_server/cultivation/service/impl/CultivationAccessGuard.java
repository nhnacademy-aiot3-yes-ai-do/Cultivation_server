package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.exception.PhotoNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.cultivationphoto.CultivationPhotoRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;


/**
 * "재배지 존재 확인 + 멤버 접근 확인" 패턴을 한 곳에 모은 공용 가드.
 * HarvestServiceImpl, CultivationPhotoServiceImpl 등에서 개별 구현하던
 * 검증 로직을 대체한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationAccessGuard {
    private final CultivationRepository cultivationRepository;
    private final CultivationPhotoRepository cultivationPhotoRepository;
    private final CultivationMemberService cultivationMemberService;

    public Cultivation requireMember(Long cultivationId, Long userId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        cultivationMemberService.existCultivationMember(cultivationId, userId);

        return cultivation;
    }
}
