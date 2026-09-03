package site.yesaido.cultivation_server.cultivation.repository.cultivation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.sensor.dto.projection.CultivationSummaryProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface CultivationRepository extends JpaRepository<Cultivation, Long>, CultivationRepositoryCustom {
    boolean existsByUserIdAndName(Long userId, String name);

    @Query("""
            select c.id as cultivationId,
                   c.name as name,
                   identity(c.mushroomReference) as mushroomId,
                   c.cultivationStatus as status,
                   c.mode as mode,
                   count(distinct allMember.id) as memberCount,
                   owner.userId as ownerUserId,
                   viewer.role as myRole,
                   c.startedAt as startedAt,
                   c.finishedAt as finishedAt,
                   c.createdAt as createdAt
                   ,c.updatedAt as updatedAt
            from Cultivation c
            join CultivationMember viewer on viewer.cultivation = c
            left join CultivationMember allMember on allMember.cultivation = c
            left join CultivationMember owner on owner.cultivation = c
                and owner.role = site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole.OWNER
            where viewer.userId = :userId
              and c.cultivationStatus not in (
                  site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus.FINISHED,
                  site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus.DELETED
              )
            group by c.id, c.name, c.mushroomReference, c.cultivationStatus, c.mode, owner.userId, viewer.role,
                     c.startedAt, c.finishedAt, c.createdAt, c.updatedAt
            order by c.createdAt desc, c.id desc
            """)
    List<CultivationSummaryProjection> findSummaryProjectionsByMemberUserId(@Param("userId") Long userId);

    @Query("""
            select c.id as cultivationId,
                   c.name as name,
                   identity(c.mushroomReference) as mushroomId,
                   c.cultivationStatus as status,
                   c.mode as mode,
                   count(distinct allMember.id) as memberCount,
                   owner.userId as ownerUserId,
                   viewer.role as myRole,
                   c.startedAt as startedAt,
                   c.finishedAt as finishedAt,
                   c.createdAt as createdAt,
                   c.updatedAt as updatedAt
            from Cultivation c
            left join CultivationMember viewer on viewer.cultivation = c
                and viewer.userId = :userId
            left join CultivationMember allMember on allMember.cultivation = c
            left join CultivationMember owner on owner.cultivation = c
                and owner.role = site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole.OWNER
            where c.id = :cultivationId
            group by c.id, c.name, c.mushroomReference, c.cultivationStatus, c.mode, owner.userId, viewer.role,
                     c.startedAt, c.finishedAt, c.createdAt, c.updatedAt
            """)
    Optional<CultivationSummaryProjection> findDetailProjectionByUserIdAndCultivationId(
            @Param("userId") Long userId,
            @Param("cultivationId") Long cultivationId
    );
}
