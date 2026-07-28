package site.yesaido.cultivation_server.repository.cultivation.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationHistoryResponse;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.entity.cultivation.QCultivation;
import site.yesaido.cultivation_server.entity.cultivationmember.QCultivationMember;
import site.yesaido.cultivation_server.entity.harvest.QHarvest;
import site.yesaido.cultivation_server.repository.cultivation.CultivationRepositoryCustom;

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
                .where(member.userId.eq(userId))
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
    public List<CultivationHistoryResponse> findHistoryByMemberUserId(Long userId) {
        QCultivation cultivation = QCultivation.cultivation;
        QCultivationMember member = QCultivationMember.cultivationMember;
        QHarvest harvest = QHarvest.harvest;

        return queryFactory
                .select(Projections.constructor(CultivationHistoryResponse.class,
                        cultivation.id,
                        cultivation.name,
                        cultivation.mushroomReference.id,
                        cultivation.cultivationStatus,
                        harvest.harvestWeight,
                        harvest.productGrade,
                        cultivation.finishedAt))
                .from(cultivation)
                .join(member).on(member.cultivation.eq(cultivation))
                .leftJoin(harvest).on(harvest.cultivation.eq(cultivation))
                .where(member.userId.eq(userId), cultivation.cultivationStatus.eq(CultivationStatus.FINISHED))
                .distinct()
                .fetch();
    }
}
