package site.yesaido.cultivation_server.cultivation.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import site.yesaido.cultivation_server.config.QuerydslConfig;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.CultivationMember;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.cultivation.repository.cultivationmember.CultivationMemberRepository;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class CultivationMemberRepositoryTest {
    @Autowired
    private CultivationMemberRepository cultivationMemberRepository;

    @Autowired
    private MushroomReferenceRepository mushroomReferenceRepository;

    @Autowired
    private EntityManager entityManager;

    private Cultivation savedCultivation;

    @BeforeEach
    void setUp() {
        MushroomReference savedMushroom = mushroomReferenceRepository.save(new MushroomReference());

        savedCultivation = Cultivation.builder()
                .userId(1L)
                .name("멤버십 재배")
                .mushroomReference(savedMushroom)
                .build();
        entityManager.persist(savedCultivation);
    }

    @Test
    @DisplayName("existsByCultivationIdAndUserId는 등록된 멤버(OWNER/MEMBER)에 대해 true를 반환함")
    void existsByCultivationIdAndUserIdTrueForRegisteredMember() {
        saveMember(savedCultivation, 1L, MemberRole.OWNER);
        saveMember(savedCultivation, 2L, MemberRole.MEMBER);

        assertThat(cultivationMemberRepository.existsByCultivationIdAndUserId(savedCultivation.getId(), 1L)).isTrue();
        assertThat(cultivationMemberRepository.existsByCultivationIdAndUserId(savedCultivation.getId(), 2L)).isTrue();
    }

    @Test
    @DisplayName("existsByCultivationIdAndUserId는 관계없는 유저에 대해 false를 반환함")
    void existsByCultivationIdAndUserIdFalseForUnrelatedUser() {
        saveMember(savedCultivation, 1L, MemberRole.OWNER);

        assertThat(cultivationMemberRepository.existsByCultivationIdAndUserId(savedCultivation.getId(), 999L)).isFalse();
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