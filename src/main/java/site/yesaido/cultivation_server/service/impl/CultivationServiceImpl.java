package site.yesaido.cultivation_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.dto.cultivation.response.*;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.entity.mushroomreference.MushroomReference;
import site.yesaido.cultivation_server.exception.*;
import site.yesaido.cultivation_server.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.repository.cultivationmember.CultivationMemberRepository;
import site.yesaido.cultivation_server.repository.mushroomreference.MushroomReferenceRepository;
import site.yesaido.cultivation_server.service.CultivationMemberService;
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
    public void deleteCultivation(Long cultivationId, Long userId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        if (!cultivation.getUserId().equals(userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        cultivationMemberService.removeAllMembers(cultivationId);
        cultivationRepository.delete(cultivation);
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
    public List<CultivationHistoryResponse> getHistory(Long userId) {
        return cultivationRepository.findHistoryByMemberUserId(userId);
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
