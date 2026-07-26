package site.yesaido.cultivation_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationCreateResponse;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.exception.CultivationAlreadyExist;
import site.yesaido.cultivation_server.repository.CultivationRepository;
import site.yesaido.cultivation_server.service.CultivationService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationServiceImpl implements CultivationService {
    private final CultivationRepository cultivationRepository;

    @Transactional
    public CultivationCreateResponse create(CultivationCreateRequest request) {
        if (cultivationRepository.existsByName(request.name())) {
            throw new CultivationAlreadyExist(request.name());
        }
        Cultivation cultivation = Cultivation.builder()
                .name(request.name())
                .startedAt(request.startedAt() != null ? request.startedAt() : LocalDateTime.now())
                .finishedAt(request.finishedAt())
                .build();
        cultivationRepository.save(cultivation);
        return CultivationCreateResponse(cultivation.getId(), )
    }

    public
}
