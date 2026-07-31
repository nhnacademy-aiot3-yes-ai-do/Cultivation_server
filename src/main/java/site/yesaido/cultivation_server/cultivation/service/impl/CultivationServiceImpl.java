package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.*;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.exception.*;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.mushroomreference.MushroomReferenceRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.cultivation.service.CultivationService;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationServiceImpl implements CultivationService {
    private final CultivationRepository cultivationRepository;
    private final MushroomReferenceRepository mushroomReferenceRepository;
    private final CultivationMemberService cultivationMemberService;

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

        cultivationMemberService.addOwner(cultivation, userId);

        return new CultivationCreateResponse(cultivation.getId(), null, Collections.emptyList());
    }

    @Override
    public List<CultivationSummaryResponse> getCultivations(Long userId) {

        return cultivationRepository.findAllByMemberUserId(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public CultivationDetailResponse getCultivation(Long userId, Long cultivationId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));
        if (!cultivationRepository.isMember(cultivationId, userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        return toDetail(cultivation);
    }

    @Override
    @Transactional
    public CultivationFinishResponse finish(Long cultivationId, Long userId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        if (!cultivation.getUserId().equals(userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        if (cultivation.getCultivationStatus() == CultivationStatus.FINISHED) {
            throw new CultivationAlreadyFinishedException(cultivationId);
        }

        cultivation.finish();

        return new CultivationFinishResponse(cultivation.getId(), cultivation.getCultivationStatus(), cultivation.getFinishedAt());
    }

    @Override
    public Page<CultivationHistoryResponse> getHistory(Long userId, Pageable pageable) {
        Page<CultivationHistoryResponse> page = cultivationRepository.findHistoryByMemberUserId(userId, pageable);

        if (page.getContent().isEmpty() && page.getTotalElements() > 0 && pageable.getPageNumber() > 0) {
            int lastPage = page.getTotalPages() - 1;
            Pageable lastPageable = PageRequest.of(lastPage, pageable.getPageSize(), pageable.getSort());
            return cultivationRepository.findHistoryByMemberUserId(userId, lastPageable);
        }
        return page;
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

    private CultivationDetailResponse toDetail(Cultivation cultivation) {
        return new CultivationDetailResponse(
                cultivation.getId(),
                cultivation.getName(),
                cultivation.getMushroomReference().getId(),
                cultivation.getCultivationStatus(),
                cultivation.getMode(),
                cultivation.getStartedAt(),
                cultivation.getFinishedAt(),
                cultivation.getCreatedAt(),
                cultivation.getUpdatedAt()
        );
    }
}
