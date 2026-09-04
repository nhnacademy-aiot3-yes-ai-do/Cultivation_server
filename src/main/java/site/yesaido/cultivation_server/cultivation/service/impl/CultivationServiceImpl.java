package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.client.UserClient;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.*;
import site.yesaido.cultivation_server.cultivation.dto.user.UserSummaryResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.exception.*;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.cultivation.service.CultivationService;
import site.yesaido.cultivation_server.sensor.dto.projection.CultivationSummaryProjection;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationServiceImpl implements CultivationService {

    private final CultivationRepository cultivationRepository;
    private final MushroomReferenceRepository mushroomReferenceRepository;
    private final CultivationMemberService cultivationMemberService;
    private final UserClient userClient;

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
                .startedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
        cultivationRepository.save(cultivation);

        cultivationMemberService.addOwner(cultivation, userId);

        return new CultivationCreateResponse(cultivation.getId(), null, Collections.emptyList());
    }

    @Override
    public CultivationSummaryListResponse getCultivations(Long userId) {
        List<CultivationSummaryProjection> projections = cultivationRepository.findSummaryProjectionsByMemberUserId(userId);
        if (projections.isEmpty()) {
            return new CultivationSummaryListResponse(List.of());
        }

        Map<Long, String> nicknameByUserId = resolveOwnerNicknames(
                projections.stream()
                        .map(CultivationSummaryProjection::ownerUserId)
                        .filter(java.util.Objects::nonNull)
                        .toList()
        );

        List<CultivationSummaryResponse> list = projections.stream()
                .map(projection -> new CultivationSummaryResponse(
                        projection.cultivationId(),
                        projection.name(),
                        projection.mushroomId(),
                        projection.status(),
                        projection.mode(),
                        projection.memberCount() == null ? 0 : projection.memberCount().intValue(),
                        nicknameByUserId.get(projection.ownerUserId()),
                        projection.createdAt()
                ))
                .toList();

        return new CultivationSummaryListResponse(list);
    }

    @Override
    public CultivationDetailResponse getCultivation(Long userId, Long cultivationId) {
        CultivationSummaryProjection projection = cultivationRepository
                .findDetailProjectionByUserIdAndCultivationId(userId, cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));
        if (projection.myRole() == null) {
            throw new CultivationAccessDeniedException(cultivationId);
        }
        return toDetail(projection);
    }

    @Override
    public CultivationModeChangeResponse switchToHarvestMode(Long cultivationId, Long userId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));
        cultivationMemberService.verifyManagerAccess(cultivationId, userId);
        if (cultivation.getCultivationStatus() == CultivationStatus.FINISHED) {
            throw new CultivationAlreadyFinishedException(cultivationId);
        }
        if (cultivation.getCultivationStatus() == CultivationStatus.DELETED) {
            throw new CultivationAlreadyDeletedException(cultivationId);
        }
        if (cultivation.getMode() == CultivationMode.HARVEST) {
            throw new CultivationAlreadyInHarvestModeException(cultivationId);
        }

        cultivation.switchToHarvestMode();
        return new CultivationModeChangeResponse(cultivation.getId(), cultivation.getMode());
    }

    @Override
    @Transactional
    public CultivationFinishResponse finish(Long cultivationId, Long userId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        cultivationMemberService.verifyOwnerAccess(cultivationId, userId);

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

    @Override
    @Transactional
    public void deleteWithoutRole(Long cultivationId, Long userId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        cultivationMemberService.verifyOwnerAccess(cultivationId, userId, null);

        if (cultivation.getCultivationStatus() == CultivationStatus.DELETED) {
            throw new CultivationAlreadyDeletedException(cultivationId);
        }
        cultivation.delete();
    }

    @Override
    @Transactional
    public void delete(Long cultivationId, Long userId, String role) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        cultivationMemberService.verifyOwnerAccess(cultivationId, userId, role);

        if (cultivation.getCultivationStatus() == CultivationStatus.DELETED) {
            throw new CultivationAlreadyDeletedException(cultivationId);
        }
        cultivation.delete();
    }


    private CultivationDetailResponse toDetail(CultivationSummaryProjection projection) {
        return new CultivationDetailResponse(
                projection.cultivationId(),
                projection.name(),
                projection.mushroomId(),
                projection.status(),
                projection.mode(),
                projection.myRole(),
                projection.startedAt(),
                projection.finishedAt(),
                projection.createdAt(),
                projection.updatedAt()
        );
    }

    private Map<Long, String> resolveOwnerNicknames(Collection<Long> ownerIds) {
        List<Long> distinctIds = ownerIds.stream().distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userClient.getUsers(distinctIds).stream()
                .collect(Collectors.toMap(UserSummaryResponse::userId, UserSummaryResponse::nickname));
    }
}
