package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.client.UserClient;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberAddRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberRoleUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.response.MemberResponse;
import site.yesaido.cultivation_server.cultivation.dto.user.UserSummaryResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.CultivationMember;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.cultivation.exception.*;
import site.yesaido.cultivation_server.cultivation.repository.cultivationmember.CultivationMemberRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationMemberServiceImpl implements CultivationMemberService {
    private final CultivationMemberRepository cultivationMemberRepository;
    private final UserClient userClient;

    @Override
    @Transactional
    public void addOwner(Cultivation cultivation, Long userId) {
        CultivationMember cultivationMember = CultivationMember.builder()
                .userId(userId)
                .role(MemberRole.OWNER)
                .cultivation(cultivation)
                .build();
        cultivationMemberRepository.save(cultivationMember);
    }

    @Override
    @Transactional
    public void addMember(Long cultivationId, Long requesterId, MemberAddRequest request) {
        if (request.role() == MemberRole.OWNER) {
            throw new InvalidMemberRoleException();
        }
        CultivationMember owner = verifyOwner(cultivationId, requesterId);

        if (cultivationMemberRepository.existsByCultivationIdAndUserId(cultivationId, request.userId())) {
            throw new CultivationMemberAlreadyExistException(request.userId());
        }

        CultivationMember newMember = CultivationMember.builder()
                .userId(request.userId())
                .role(request.role())
                .cultivation(owner.getCultivation())
                .build();
        try {
            cultivationMemberRepository.save(newMember);
        } catch (DataIntegrityViolationException e) {
            throw new CultivationMemberAlreadyExistException(request.userId());
        }
    }

    @Override
    public List<MemberResponse> getMembers(Long cultivationId, Long requesterId) {
        existCultivationMember(cultivationId, requesterId);

        List<CultivationMember> members = cultivationMemberRepository.findAllByCultivationId(cultivationId);
        if (members.isEmpty()) {
            return List.of();
        }

        Map<Long, String> nicknameByUserId = userClient.getUsers(
                members.stream().map(CultivationMember::getUserId).toList()
        ).stream().collect(Collectors.toMap(UserSummaryResponse::userId, UserSummaryResponse::nickname));

        return members.stream()
                .map(member -> toResponse(member, nicknameByUserId.get(member.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public void updateMember(Long cultivationId, Long requesterId, Long targetUserId, MemberRoleUpdateRequest request) {
        if (request.role() == MemberRole.OWNER) {
            throw new InvalidMemberRoleException();
        }
        verifyOwner(cultivationId, requesterId);

        CultivationMember target = cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, targetUserId)
                .orElseThrow(CultivationMemberNotFoundException::new);

        if (target.getRole() == MemberRole.OWNER) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        target.updateRole(request.role());
    }

    @Override
    @Transactional
    public void removeMember(Long cultivationId, Long requesterId, Long targetUserId) {
        verifyOwner(cultivationId, requesterId);

        CultivationMember target = cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, targetUserId)
                .orElseThrow(CultivationMemberNotFoundException::new);

        if (target.getRole() == MemberRole.OWNER) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        cultivationMemberRepository.delete(target);
    }

    @Override
    @Transactional
    public void transferOwnership(Long cultivationId, Long requesterId, Long newUserId) {
        if (requesterId.equals(newUserId)) {
            throw new InvalidOwnershipTransferException();
        }

        CultivationMember currentOwner = cultivationMemberRepository.findByCultivationIdAndUserIdForUpdate(cultivationId, requesterId)
                .orElseThrow(CultivationMemberNotFoundException::new);

        if (currentOwner.getRole() != MemberRole.OWNER) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        CultivationMember newOwner = cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, newUserId)
                .orElseThrow(CultivationMemberNotFoundException::new);

        currentOwner.updateRole(MemberRole.MANAGER);
        newOwner.updateRole(MemberRole.OWNER);
        currentOwner.getCultivation().changeOwner(newUserId);
    }

    @Override
    public void existCultivationMember(Long cultivationId, Long userId) {
        if (!cultivationMemberRepository.existsByCultivationIdAndUserId(cultivationId, userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }
    }

    // Helper Method
    private CultivationMember verifyOwner(Long cultivationId, Long userId) {
        CultivationMember member = cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, userId)
                .orElseThrow(CultivationMemberNotFoundException::new);

        if (member.getRole() != MemberRole.OWNER) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        return member;
    }

    private MemberResponse toResponse(CultivationMember member, String nickname) {
        return new MemberResponse(
                member.getId(),
                member.getUserId(),
                nickname,
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
