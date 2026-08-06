package site.yesaido.cultivation_server.sensor.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import site.yesaido.cultivation_server.sensor.entity.QEnvironmentComplianceStat;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentComplianceStatRepositoryCustom;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class EnvironmentComplianceStatRepositoryImpl implements EnvironmentComplianceStatRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Long incrementInRange(Long cultivationId, Long sensorTypeId) {
        QEnvironmentComplianceStat stat = QEnvironmentComplianceStat.environmentComplianceStat;

        return queryFactory
                .update(stat)
                .set(stat.inRangeCount, stat.inRangeCount.add(1))
                .set(stat.updatedAt, LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .where(stat.cultivationId.eq(cultivationId), stat.sensorType.id.eq(sensorTypeId))
                .execute();
    }

    @Override
    public Long incrementOutOfRange(Long cultivationId, Long sensorTypeId) {
        QEnvironmentComplianceStat stat = QEnvironmentComplianceStat.environmentComplianceStat;

        return queryFactory
                .update(stat)
                .set(stat.outOfRangeCount, stat.outOfRangeCount.add(1))
                .set(stat.updatedAt, LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .where(stat.cultivationId.eq(cultivationId), stat.sensorType.id.eq(sensorTypeId))
                .execute();
    }
}
