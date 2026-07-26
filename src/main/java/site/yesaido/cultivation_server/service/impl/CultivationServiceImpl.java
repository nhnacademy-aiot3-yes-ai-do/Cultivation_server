package site.yesaido.cultivation_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationCreateResponse;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationSummaryResponse;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.entity.mushroomreference.MushroomReference;
import site.yesaido.cultivation_server.exception.CultivationAlreadyExist;
import site.yesaido.cultivation_server.exception.MushroomNotFoundException;
import site.yesaido.cultivation_server.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.repository.mushroomreference.MushroomReferenceRepository;
import site.yesaido.cultivation_server.service.CultivationService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationServiceImpl implements CultivationService {
    private final CultivationRepository cultivationRepository;
    private final MushroomReferenceRepository mushroomReferenceRepository;

    @Override
    @Transactional
    public CultivationCreateResponse create(CultivationCreateRequest request, Long userId) {
        if (cultivationRepository.existsByUserIdAndName(userId, request.name())) {
            throw new CultivationAlreadyExist(request.name());
        }

        MushroomReference mushroomReference = mushroomReferenceRepository.findById(request.mushroomId())
                .orElseThrow(() -> new MushroomNotFoundException(request.mushroomId()));

        Cultivation cultivation = Cultivation.builder()
                .name(request.name())
                .userId(userId)
                .mushroomReference(mushroomReference)
                .startedAt(LocalDateTime.now())
                .build();
        cultivationRepository.save(cultivation);

        return new CultivationCreateResponse(cultivation.getId(), null, Collections.emptyList());
    }

    @Override
    public List<CultivationSummaryResponse> getCultivations(Long userId) {

        return cultivationRepository.findAllByMemberUserId(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    // Helper Method
    private CultivationSummaryResponse toSummary(Cultivation cultivation) {
        return new CultivationSummaryResponse(
                cultivation.getId(),
                cultivation.getName(),
                cultivation.getMushroomReference().getId(),
                cultivation.getCultivationStatus(),
                cultivation.getMode(),
                cultivation.getCreatedAt());
    }
}
