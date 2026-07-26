package site.yesaido.cultivation_server.repository.cultivation.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.entity.cultivation.QCultivation;
import site.yesaido.cultivation_server.entity.cultivationmember.QCultivationMember;
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
}
