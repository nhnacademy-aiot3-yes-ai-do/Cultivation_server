package site.yesaido.cultivation_server.cultivation.repository.cultivationmember;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.CultivationMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface CultivationMemberRepository extends CrudRepository<CultivationMember, Long> {
    boolean existsByCultivationIdAndUserId(Long cultivationId, Long userId);
    Optional<CultivationMember> findByCultivationIdAndUserId(Long cultivationId, Long userId);
    List<CultivationMember> findAllByCultivationId(Long cultivationId);

    // 락 적용 OWNER 승격
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM CultivationMember m WHERE m.cultivation.id = :cultivationId AND m.userId = :userId")
    Optional<CultivationMember> findByCultivationIdAndUserIdForUpdate(@Param("cultivationId") Long cultivationId,
                                                                      @Param("userId") Long userId);

    List<CultivationMember> findAllByCultivationIdIn(List<Long> cultivationIds);
}
