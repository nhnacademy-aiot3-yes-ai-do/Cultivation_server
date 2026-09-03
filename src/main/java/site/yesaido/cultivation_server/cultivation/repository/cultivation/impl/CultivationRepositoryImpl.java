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
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.QCultivationMember;
import site.yesaido.cultivation_server.cultivation.entity.harvest.QHarvest;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepositoryCustom;
import site.yesaido.cultivation_server.sensor.dto.projection.CultivationSummaryProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Override
    public List<CultivationSummaryProjection> findSummaryProjectionsByMemberUserId(Long userId) {
        QCultivation cultivation = QCultivation.cultivation;
        QCultivationMember viewer = new QCultivationMember("viewer");
        QCultivationMember allMember = new QCultivationMember("allMember");
        QCultivationMember owner = new QCultivationMember("owner");

        return queryFactory
                .select(Projections.constructor(CultivationSummaryProjection.class,
                        cultivation.id,
                        cultivation.name,
                        cultivation.mushroomReference.id,
                        cultivation.cultivationStatus,
                        cultivation.mode,
                        allMember.id.countDistinct(),                                                                   // COUNT(DISTINCT all_member.id)
                        owner.userId,
                        viewer.role,
                        cultivation.startedAt,
                        cultivation.finishedAt,
                        cultivation.createdAt,
                        cultivation.updatedAt))
                .from(cultivation)
                .join(viewer).on(viewer.cultivation.eq(cultivation))
                .leftJoin(allMember).on(allMember.cultivation.eq(cultivation))                                          // 해당 재배에 속해 있는 모든 멤버를 연결
                .leftJoin(owner).on(owner.cultivation.eq(cultivation).and(owner.role.eq(MemberRole.OWNER)))             // 현재 재배와 연결된 멤버를 찾고 그 중에 역할이 OWNER인 사람만 가져옴
                .where(viewer.userId.eq(userId),                                                                        // 현재 사용자가 해당 재배의 멤버여야 함
                        cultivation.cultivationStatus.notIn(CultivationStatus.FINISHED, CultivationStatus.DELETED))     // 재배 상태가 FINISHED, DELETED인 것을 제외함
                .groupBy(cultivation.id, cultivation.name, cultivation.mushroomReference, cultivation.cultivationStatus,
                        cultivation.mode, owner.userId, viewer.role,
                        cultivation.startedAt, cultivation.finishedAt, cultivation.createdAt, cultivation.updatedAt)    // countDistinct란 집계함수를 사용했기 때문에 GROUP BY를 사용해야함.
                .orderBy(cultivation.createdAt.desc(), cultivation.id.desc())                                           // 기준 1. 생성일 내림차순(최신 생성된 재배), 2. ID 내림차순
                .fetch();
    }

    @Override
    public Optional<CultivationSummaryProjection> findDetailProjectionByUserIdAndCultivationId(Long userId, Long cultivationId) {
        QCultivation cultivation = QCultivation.cultivation;
        QCultivationMember viewer = new QCultivationMember("viewer");
        QCultivationMember allMember = new QCultivationMember("allMember");
        QCultivationMember owner = new QCultivationMember("owner");

        CultivationSummaryProjection result = queryFactory
                .select(Projections.constructor(CultivationSummaryProjection.class,
                        cultivation.id,
                        cultivation.name,
                        cultivation.mushroomReference.id,
                        cultivation.cultivationStatus,
                        cultivation.mode,
                        allMember.id.countDistinct(),                                                                   // 해당 재배의 전체 멤버수를 셈
                        owner.userId,
                        viewer.role,
                        cultivation.startedAt,
                        cultivation.finishedAt,
                        cultivation.createdAt,
                        cultivation.updatedAt))
                .from(cultivation)
                .leftJoin(viewer).on(viewer.cultivation.eq(cultivation).and(viewer.userId.eq(userId)))                  // 현재 userId가 이 재배에 참여했다면 그 사람의 role을 가져와라
                .leftJoin(allMember).on(allMember.cultivation.eq(cultivation))
                .leftJoin(owner).on(owner.cultivation.eq(cultivation).and(owner.role.eq(MemberRole.OWNER)))             // OWNER의 userId를 찾음
                .where(cultivation.id.eq(cultivationId))                                                                // 특정 cultivationId의 재배를 찾음
                .groupBy(cultivation.id, cultivation.name, cultivation.mushroomReference, cultivation.cultivationStatus,
                        cultivation.mode, owner.userId, viewer.role,
                        cultivation.startedAt, cultivation.finishedAt, cultivation.createdAt, cultivation.updatedAt)
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
