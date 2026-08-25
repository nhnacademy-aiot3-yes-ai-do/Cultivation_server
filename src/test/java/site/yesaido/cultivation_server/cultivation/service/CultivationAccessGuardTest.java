package site.yesaido.cultivation_server.cultivation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationAccessGuard;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CultivationAccessGuardTest {

    @Mock private CultivationRepository cultivationRepository;
    @Mock private CultivationMemberService cultivationMemberService;

    @InjectMocks
    private CultivationAccessGuard cultivationAccessGuard;

    @Test
    @DisplayName("requireMember 성공 - 존재하는 재배 + 멤버")
    void requireMemberSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));

        Cultivation result = cultivationAccessGuard.requireMember(cultivationId, userId);

        assertThat(result).isEqualTo(cultivation);
    }

    @Test
    @DisplayName("requireMember 실패 - 존재하지 않는 재배")
    void requireMemberFailCultivationNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cultivationAccessGuard.requireMember(cultivationId, userId))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("requireMember 실패 - 멤버가 아닌 경우")
    void requireMemberFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(2L).name("버섯 농장").build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        doThrow(new CultivationAccessDeniedException(cultivationId))
                .when(cultivationMemberService).existCultivationMember(cultivationId, userId);

        assertThatThrownBy(() -> cultivationAccessGuard.requireMember(cultivationId, userId))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }
}