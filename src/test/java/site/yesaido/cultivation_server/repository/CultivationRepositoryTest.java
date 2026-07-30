package site.yesaido.cultivation_server.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import site.yesaido.cultivation_server.config.QuerydslConfig;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationHistoryResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.CultivationMember;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.mushroomreference.MushroomReferenceRepository;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class CultivationRepositoryTest {
    @Autowired
    private CultivationRepository cultivationRepository;

    @Autowired
    private MushroomReferenceRepository mushroomReferenceRepository;

    @Autowired
    private EntityManager entityManager;

    private MushroomReference savedMushroom;

    @BeforeEach
    void setUp() {
        MushroomReference mushroom = new MushroomReference();
        savedMushroom = mushroomReferenceRepository.save(mushroom);
    }

    @Test
    @DisplayName("경작 엔티티가 적상적으로 저장됨")
    void saveCultivationSuccess() {
        Cultivation cultivation = Cultivation.builder()
                .userId(1L)
                .name("테스트 경작")
                .mushroomReference(savedMushroom)
                .build();

        Cultivation savedCultivation = cultivationRepository.save(cultivation);

        assertThat(savedCultivation.getId()).isNotNull();
        assertThat(savedCultivation.getName()).isEqualTo(cultivation.getName());
        assertThat(savedCultivation.getUserId()).isEqualTo(cultivation.getUserId());
        assertThat(savedCultivation.getCultivationStatus()).isNotNull();
    }

    @Test
    @DisplayName("유저 ID와 경작 이름으로 존재 여부를 정확히 판단함")
    void existsByUserIdAndNameSuccess() {
        Cultivation cultivation = Cultivation.builder()
                .userId(1L)
                .name("테스트 버섯")
                .mushroomReference(savedMushroom)
                .build();
        cultivationRepository.save(cultivation);

        boolean exists = cultivationRepository.existsByUserIdAndName(cultivation.getUserId(), cultivation.getName());
        boolean notExistsName = cultivationRepository.existsByUserIdAndName(cultivation.getUserId(), "없는 이름");
        boolean notExistUser = cultivationRepository.existsByUserIdAndName(999L, cultivation.getName());

        assertThat(exists).isTrue();
        assertThat(notExistsName).isFalse();
        assertThat(notExistUser).isFalse();
    }

    @Test
    @DisplayName("동일한 유저가 같은 이름의 경작을 중복 생성할 경우 예외 발생")
    void saveCultivationDuplicateNameThrowsException() {
        Cultivation cultivation1 = Cultivation.builder()
                .userId(1L)
                .name("중복 버섯")
                .mushroomReference(savedMushroom)
                .build();
        cultivationRepository.save(cultivation1);

        Cultivation cultivation2 = Cultivation.builder()
                .userId(1L)
                .name("중복 버섯")
                .mushroomReference(savedMushroom)
                .build();
        assertThatThrownBy(() -> {
            cultivationRepository.save(cultivation2);
            cultivationRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("findAllByMemberUserId는 OWNER로 속한 재배를 반환함")
    void findAllByMemberUserIdReturnsCultivationForOwner() {
        Cultivation cultivation = cultivationRepository.save(
                Cultivation.builder().userId(1L).name("오너 재배").mushroomReference(savedMushroom).build());
        saveMember(cultivation, 1L, MemberRole.OWNER);

        List<Cultivation> result = cultivationRepository.findAllByMemberUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(cultivation.getId());
    }

    @Test
    @DisplayName("findAllByMemberUserId는 MEMBER로 초대된 재배도 반환함")
    void findAllByMemberUserIdReturnsCultivationForMember() {
        Cultivation cultivation = cultivationRepository.save(
                Cultivation.builder().userId(1L).name("초대된 재배").mushroomReference(savedMushroom).build());
        saveMember(cultivation, 1L, MemberRole.OWNER);
        saveMember(cultivation, 2L, MemberRole.MEMBER);

        List<Cultivation> result = cultivationRepository.findAllByMemberUserId(2L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(cultivation.getId());
    }

    @Test
    @DisplayName("findAllByMemberUserId는 관계없는 유저의 재배는 반환하지 않음")
    void findAllByMemberUserIdExcludesUnrelatedUser() {
        Cultivation cultivation = cultivationRepository.save(
                Cultivation.builder().userId(1L).name("남의 재배").mushroomReference(savedMushroom).build());
        saveMember(cultivation, 1L, MemberRole.OWNER);

        List<Cultivation> result = cultivationRepository.findAllByMemberUserId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isMember는 등록된 멤버(OWNER/MEMBER)에 대해 true를 반환함")
    void isMemberTrueForRegisteredMember() {
        Cultivation cultivation = cultivationRepository.save(
                Cultivation.builder().userId(1L).name("멤버십 재배").mushroomReference(savedMushroom).build());
        saveMember(cultivation, 1L, MemberRole.OWNER);
        saveMember(cultivation, 2L, MemberRole.MEMBER);

        assertThat(cultivationRepository.isMember(cultivation.getId(), 1L)).isTrue();
        assertThat(cultivationRepository.isMember(cultivation.getId(), 2L)).isTrue();
    }

    @Test
    @DisplayName("isMember는 관계없는 유저에 대해 false를 반환함")
    void isMemberFalseForUnrelatedUser() {
        Cultivation cultivation = cultivationRepository.save(
                Cultivation.builder().userId(1L).name("비멤버 재배").mushroomReference(savedMushroom).build());
        saveMember(cultivation, 1L, MemberRole.OWNER);

        assertThat(cultivationRepository.isMember(cultivation.getId(), 999L)).isFalse();
    }

    @Test
    @DisplayName("페이징 경계값 - 21개 데이터 삽입 시 2번째 페이지는 1개만 조회됨")
    void findHistoryByMemberUserIdPagingBoundary() {
        Long userId = 1L;
        for (int i = 1; i <= 21; i++) {
            Cultivation cultivation = Cultivation.builder()
                    .userId(userId)
                    .name("종료된 농장 " + i)
                    .mushroomReference(savedMushroom)
                    .cultivationStatus(CultivationStatus.FINISHED)
                    .build();
            entityManager.persist(cultivation);

            CultivationMember member = CultivationMember.builder()
                    .userId(userId)
                    .role(MemberRole.OWNER)
                    .cultivation(cultivation)
                    .build();
            entityManager.persist(member);
        }
        entityManager.flush();
        entityManager.clear();

        PageRequest page0 = PageRequest.of(0, 20);
        Page<CultivationHistoryResponse> resultPage0 = cultivationRepository.findHistoryByMemberUserId(userId, page0);

        PageRequest page1 = PageRequest.of(1, 20);
        Page<CultivationHistoryResponse> resultPage1 = cultivationRepository.findHistoryByMemberUserId(userId, page1);

        assertThat(resultPage0.getContent()).hasSize(20);
        assertThat(resultPage0.getTotalElements()).isEqualTo(21);
        assertThat(resultPage0.getTotalPages()).isEqualTo(2);

        assertThat(resultPage1.getContent()).hasSize(1);
    }

    // Helper Method
    private void saveMember(Cultivation cultivation, Long userId, MemberRole role) {
        CultivationMember member = CultivationMember.builder()
                .cultivation(cultivation)
                .userId(userId)
                .role(role)
                .build();
        entityManager.persist(member);
    }
}
