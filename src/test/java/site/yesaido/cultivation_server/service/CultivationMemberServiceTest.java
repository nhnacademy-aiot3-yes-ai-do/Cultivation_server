package site.yesaido.cultivation_server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.dto.cultivationmember.request.MemberAddRequest;
import site.yesaido.cultivation_server.dto.cultivationmember.request.MemberRoleUpdateRequest;
import site.yesaido.cultivation_server.dto.cultivationmember.response.MemberResponse;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.entity.cultivationmember.CultivationMember;
import site.yesaido.cultivation_server.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.exception.CultivationMemberNotFoundException;
import site.yesaido.cultivation_server.exception.InvalidMemberRoleException;
import site.yesaido.cultivation_server.exception.InvalidOwnershipTransferException;
import site.yesaido.cultivation_server.repository.cultivationmember.CultivationMemberRepository;
import site.yesaido.cultivation_server.service.impl.CultivationMemberServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CultivationMemberServiceTest {
    @Mock
    private CultivationMemberRepository cultivationMemberRepository;

    @InjectMocks
    private CultivationMemberServiceImpl cultivationMemberService;

    @Test
    @DisplayName("멤버 추가 실패 - 요청자가 방장이 아님")
    void addMemberFailNotOwner() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        MemberAddRequest request = new MemberAddRequest(200L, MemberRole.MEMBER);

        CultivationMember member = CultivationMember.builder()
                .userId(requesterId)
                .role(MemberRole.MEMBER)
                .build();

        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId,requesterId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> cultivationMemberService.addMember(cultivationId, requesterId, request)).isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("멤버 추가 실패 - OWNER 권한으로 추가 시 에러")
    void addMemberFailInvalidRole() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        MemberAddRequest request = new MemberAddRequest(200L, MemberRole.OWNER);

        assertThatThrownBy(() -> cultivationMemberService.addMember(cultivationId, requesterId, request)).isInstanceOf(InvalidMemberRoleException.class);
    }

    @Test
    @DisplayName("멤버 추가 성공")
    void addMemberSuccess() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        MemberAddRequest request = new MemberAddRequest(200L, MemberRole.MEMBER);

        Cultivation cultivation = Cultivation.builder().id(cultivationId).build();
        CultivationMember owner = CultivationMember.builder()
                .userId(requesterId)
                .role(MemberRole.OWNER)
                .cultivation(cultivation)
                .build();

        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId,requesterId)).thenReturn(Optional.of(owner));
        when(cultivationMemberRepository.existsByCultivationIdAndUserId(cultivationId, request.userId())).thenReturn(false);

        cultivationMemberService.addMember(cultivationId, requesterId, request);

        verify(cultivationMemberRepository, times(1)).save(any(CultivationMember.class));
    }

    @Test
    @DisplayName("멤버 목록 조회 성공")
    void getMembersSuccess() {
        Long cultivationId = 1L;
        Long requesterId = 100L;

        CultivationMember member1 = CultivationMember.builder().userId(100L).role(MemberRole.OWNER).build();
        CultivationMember member2 = CultivationMember.builder().userId(200L).role(MemberRole.MEMBER).build();

        when(cultivationMemberRepository.existsByCultivationIdAndUserId(cultivationId,requesterId)).thenReturn(true);
        when(cultivationMemberRepository.findAllByCultivationId(cultivationId)).thenReturn(List.of(member1, member2));

        List<MemberResponse> responses = cultivationMemberService.getMembers(cultivationId, requesterId);

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().userId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("멤버 목록 조회 실패 - 소속되지 않은 유저가 조회 요청 시 예외 발생")
    void getMembersFailNotMember() {
        Long cultivationId = 1L;
        Long outsiderId = 999L;

        when(cultivationMemberRepository.existsByCultivationIdAndUserId(cultivationId, outsiderId)).thenReturn(false);
        assertThatThrownBy(() -> cultivationMemberService.getMembers(cultivationId, outsiderId)).isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("멤버 권한 수정 성공 - 대상 멤버의 권한이 정상 변경됨")
    void updateMembersSuccess() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        Long targetUserId = 200L;
        MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(MemberRole.MEMBER);

        CultivationMember owner = CultivationMember.builder().userId(requesterId).role(MemberRole.OWNER).build();
        CultivationMember targetMember = CultivationMember.builder().userId(targetUserId).role(MemberRole.MEMBER).build();

        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, requesterId)).thenReturn(Optional.of(owner));
        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, targetUserId)).thenReturn(Optional.of(targetMember));

        cultivationMemberService.updateMember(cultivationId, requesterId, targetUserId, request);

        assertThat(targetMember.getRole()).isEqualTo(MemberRole.MEMBER);
    }

    @Test
    @DisplayName("멤버 권한 수정 실패 - 방장 권한을 수정하려고 하면 예외 발생")
    void updateMemberFailTargetIsOwner() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        Long targetUserId = 200L;
        MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(MemberRole.MEMBER);

        CultivationMember owner = CultivationMember.builder().userId(requesterId).role(MemberRole.OWNER).build();
        CultivationMember targetOwner = CultivationMember.builder().userId(targetUserId).role(MemberRole.OWNER).build();

        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, requesterId)).thenReturn(Optional.of(owner));
        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, targetUserId)).thenReturn(Optional.of(targetOwner));

        assertThatThrownBy(() -> cultivationMemberService.updateMember(cultivationId, requesterId, targetUserId, request)).isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("멤버 삭제 성공")
    void removeMemberSuccess() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        Long targetUserId = 200L;

        CultivationMember owner = CultivationMember.builder().userId(requesterId).role(MemberRole.OWNER).build();
        CultivationMember targetMember = CultivationMember.builder().userId(targetUserId).role(MemberRole.MEMBER).build();

        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, requesterId)).thenReturn(Optional.of(owner));
        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, targetUserId)).thenReturn(Optional.of(targetMember));

        cultivationMemberService.removeMember(cultivationId, requesterId, targetUserId);

        verify(cultivationMemberRepository, times(1)).delete(targetMember);
    }

    @Test
    @DisplayName("방장 권한 멤버 추가 성공 - 리포지토리에 save가 올바른 엔티티로 호출됨")
    void addOwnerSuccess() {
        Long userId = 100L;
        Cultivation cultivation = Cultivation.builder().id(1L).build();

        cultivationMemberService.addOwner(cultivation, userId);

        ArgumentCaptor<CultivationMember> captor = ArgumentCaptor.forClass(CultivationMember.class);
        verify(cultivationMemberRepository, times(1)).save(captor.capture());

        CultivationMember savedMember= captor.getValue();
        assertThat(savedMember.getUserId()).isEqualTo(userId);
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.OWNER);
        assertThat(savedMember.getCultivation()).isEqualTo(cultivation);
    }

    @Test
    @DisplayName("방장 위임 실패 - 자기 자신에게 위임하려고 시도 시 예외 발생")
    void transferOwnershipFailSelfTransfer() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        Long newUserId = 100L;

        assertThatThrownBy(() -> cultivationMemberService.transferOwnership(cultivationId, requesterId, newUserId)).isInstanceOf(InvalidOwnershipTransferException.class);
    }

    @Test
    @DisplayName("방장 위임 실패 - 양도받을 대상이 현재 경작의 멤버가 아님")
    void transferOwnershipFailTargetNotFound() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        Long newUserId = 200L;

        CultivationMember currentOwner = CultivationMember.builder().userId(requesterId).role(MemberRole.OWNER).build();
        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, requesterId)).thenReturn(Optional.of(currentOwner));
        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, newUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cultivationMemberService.transferOwnership(cultivationId, requesterId, newUserId)).isInstanceOf(CultivationMemberNotFoundException.class);
    }

    @Test
    @DisplayName("방장 위임 성공 - 기존 방장은 MANAGER, 새 방장은 OWNER로 변경되며 Cultivation 소유자도 변경됨")
    void transferOwnershipSuccess() {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        Long newUserId = 200L;

        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(requesterId).build();

        CultivationMember currentOwner = CultivationMember.builder()
                .userId(requesterId)
                .role(MemberRole.OWNER)
                .cultivation(cultivation)
                .build();

        CultivationMember newOwner = CultivationMember.builder()
                .userId(newUserId)
                .role(MemberRole.MEMBER)
                .cultivation(cultivation)
                .build();

        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, requesterId)).thenReturn(Optional.of(currentOwner));
        when(cultivationMemberRepository.findByCultivationIdAndUserId(cultivationId, newUserId)).thenReturn(Optional.of(newOwner));

        cultivationMemberService.transferOwnership(cultivationId, requesterId, newUserId);

        assertThat(currentOwner.getRole()).isEqualTo(MemberRole.MANAGER);
        assertThat(newOwner.getRole()).isEqualTo(MemberRole.OWNER);

        assertThat(cultivation.getUserId()).isEqualTo(newUserId);
    }
}
