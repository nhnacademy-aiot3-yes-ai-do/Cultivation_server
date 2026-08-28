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
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.CultivationMember;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.cultivation.exception.*;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.cultivationmember.CultivationMemberRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.cultivation.service.CultivationService;
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
    private static final String ADMIN_ROLE = "ADMIN";

    private final CultivationRepository cultivationRepository;
    private final CultivationMemberRepository cultivationMemberRepository;
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
        List<Cultivation> cultivations = cultivationRepository.findAllByMemberUserId(userId);
        if (cultivations.isEmpty()) {
            return new CultivationSummaryListResponse(List.of());
        }

        List<Long> cultivationIds = cultivations.stream().map(Cultivation::getId).toList();
        List<CultivationMember> members = cultivationMemberRepository.findAllByCultivationIdIn(cultivationIds);

        Map<Long, Long> memberCountByCultivationId = members.stream()
                .collect(Collectors.groupingBy(m -> m.getCultivation().getId(), Collectors.counting()));

        Map<Long, Long> ownerIdByCultivationId = members.stream()
                .filter(m -> m.getRole() == MemberRole.OWNER)
                .collect(Collectors.toMap(m -> m.getCultivation().getId(), CultivationMember::getUserId));

        Map<Long, String> nicknameByUserId = resolveOwnerNicknames(ownerIdByCultivationId.values());

        List<CultivationSummaryResponse> list = cultivations.stream()
                .map(c -> {
                    Long ownerId = ownerIdByCultivationId.get(c.getId());
                    String ownerNickname = ownerId != null ? nicknameByUserId.get(ownerId) : null;
                    return toSummary(
                            c,
                            memberCountByCultivationId.getOrDefault(c.getId(), 0L).intValue(),
                            ownerNickname
                    );
                })
                .toList();

        return new CultivationSummaryListResponse(list);
    }

    @Override
    public CultivationDetailResponse getCultivation(Long userId, Long cultivationId) {
        return getCultivation(userId, cultivationId, null);
    }

    @Override
    public CultivationDetailResponse getCultivation(Long userId, Long cultivationId, String role) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        if (ADMIN_ROLE.equals(role)) {
            return toDetail(cultivation, null);
        }

        CultivationMember member = cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, userId)
                .orElseThrow(() -> new CultivationAccessDeniedException(cultivationId));

        return toDetail(cultivation, member.getRole());
    }

    @Override
    public CultivationModeChangeResponse switchToHarvestMode(Long cultivationId, Long userId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));
        cultivationMemberService.verifyManagerAccess(cultivationId, userId);
        if (cultivation.getCultivationStatus() == CultivationStatus.FINISHED) {
            throw new CultivationAlreadyFinishedException(cultivationId);
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
    public void delete(Long cultivationId, Long userId) {
        delete(cultivationId, userId, null);
    }

    @Override
    public void delete(Long cultivationId, Long userId, String role) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        cultivationMemberService.verifyOwnerAccess(cultivationId, userId, role);

        if (cultivation.getCultivationStatus() == CultivationStatus.DELETED) {
            throw new CultivationAlreadyDeletedException(cultivationId);
        }
        cultivation.delete();
    }

    // Helper Method
    private CultivationSummaryResponse toSummary(Cultivation cultivation, int memberCount, String ownerNickname) {
        return new CultivationSummaryResponse(
                cultivation.getId(),
                cultivation.getName(),
                cultivation.getMushroomReference().getId(),
                cultivation.getCultivationStatus(),
                cultivation.getMode(),
                memberCount,
                ownerNickname,
                cultivation.getCreatedAt());
    }

    private CultivationDetailResponse toDetail(Cultivation cultivation, MemberRole myRole) {
        return new CultivationDetailResponse(
                cultivation.getId(),
                cultivation.getName(),
                cultivation.getMushroomReference().getId(),
                cultivation.getCultivationStatus(),
                cultivation.getMode(),
                myRole,
                cultivation.getStartedAt(),
                cultivation.getFinishedAt(),
                cultivation.getCreatedAt(),
                cultivation.getUpdatedAt()
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
