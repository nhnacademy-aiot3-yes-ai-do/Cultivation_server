package site.yesaido.cultivation_server.repository.cultivation.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public Page<CultivationHistoryResponse> findHistoryByMemberUserId(Long userId, Pageable pageable) {
        QCultivation cultivation = QCultivation.cultivation;
        QCultivationMember member = QCultivationMember.cultivationMember;
        QHarvest harvest = QHarvest.harvest;

        List<CultivationHistoryResponse> content = queryFactory
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
                .orderBy(cultivation.finishedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(cultivation.countDistinct())
                .from(cultivation)
                .join(member).on(member.cultivation.eq(cultivation))
                .where(member.userId.eq(userId), cultivation.cultivationStatus.eq(CultivationStatus.FINISHED))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
