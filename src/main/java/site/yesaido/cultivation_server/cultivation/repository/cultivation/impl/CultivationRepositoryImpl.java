package site.yesaido.cultivation_server.cultivation.repository.cultivation.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationHistoryResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.QCultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.QCultivationMember;
import site.yesaido.cultivation_server.cultivation.entity.harvest.QHarvest;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepositoryCustom;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class CultivationRepositoryImpl implements CultivationRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Cultivation> findAllByMemberUserId(Long userId) {
        QCultivation cultivation = QCultivation.cultivation;
        QCultivationMember member = QCultivationMember.cultivationMember;

        return queryFactory
                .selectFrom(cultivation)
                .join(cultivation.mushroomReference).fetchJoin()
                .join(member).on(member.cultivation.eq(cultivation))
                .where(member.userId.eq(userId),
                        cultivation.cultivationStatus.notIn(CultivationStatus.FINISHED, CultivationStatus.DELETED))
                .distinct()
                .fetch();
    }

    @Override
    public boolean isMember(Long cultivationId, Long userId) {
        QCultivationMember member = QCultivationMember.cultivationMember;
        return queryFactory
                .selectOne()
                .from(member)
                .where(member.cultivation.id.eq(cultivationId), member.userId.eq(userId))
                .fetchFirst() != null;
    }

    @Override
    public Page<CultivationHistoryResponse> findHistoryByMemberUserId(Long userId, Pageable pageable) {
        QCultivation cultivation = QCultivation.cultivation;
        QCultivationMember member = QCultivationMember.cultivationMember;
        QHarvest harvest = QHarvest.harvest;

        /*
        CASE
            WHEN c.finished_at IS NOT NULL THEN c.finished_at
            ELSE c.deleted_at
        END AS ended_at
        * */
        DateTimeExpression<LocalDateTime> endedAt = new CaseBuilder()
                .when(cultivation.finishedAt.isNotNull()).then(cultivation.finishedAt)
                .otherwise(cultivation.deletedAt);

        List<CultivationHistoryResponse> content = queryFactory
                .select(Projections.constructor(CultivationHistoryResponse.class,                                       // DB에서 데이터들을 가져와서 이 DTO로 만들어줘.
                cultivation.id,
                cultivation.name,
                cultivation.mushroomReference.id,
                cultivation.cultivationStatus,
                harvest.harvestWeight,
                harvest.productGrade,
                endedAt))
                .from(cultivation)                                                                                      // 어떤 테이블을 기준으로 조회할 것인가?
                .join(member).on(member.cultivation.eq(cultivation))                                                    // Member와 Cultivation 테이블을 연결해서 조회하겠다. on은 연결 조건 (현재 조회 중인 Cultivation과 Member의 Cultivation이 같은것만 가져옴)
                .leftJoin(harvest).on(harvest.cultivation.eq(cultivation))                                              // Cultivation에 연결된 Harvest가 있으면 가져와라. Harvest가 없어도 Cultivation을 조회하겠다.
                .where(member.userId.eq(userId), cultivation.cultivationStatus.
                        in(CultivationStatus.FINISHED, CultivationStatus.DELETED))                                      // 현재 로그인한 사용자의 재배만 가져와라. 그리고 재배가 끝난 재배지만 가져와라.
                .orderBy(endedAt.desc(), cultivation.id.desc())                                                         // 종료시간이 최신순, 두번째로 ID가 큰것부터
                .offset(pageable.getOffset())                                                                           // 몇 개를 건너뛸 것인가?
                .limit(pageable.getPageSize())                                                                          // 몇 개를 가져올건지
                .fetch();                                                                                               // 쿼리를 실행하고 결과를 가져와라

        Long total = queryFactory
                .select(cultivation.countDistinct())
                .from(cultivation)
                .join(member).on(member.cultivation.eq(cultivation))
                .where(member.userId.eq(userId), cultivation.cultivationStatus
                        .in(CultivationStatus.FINISHED, CultivationStatus.DELETED))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
