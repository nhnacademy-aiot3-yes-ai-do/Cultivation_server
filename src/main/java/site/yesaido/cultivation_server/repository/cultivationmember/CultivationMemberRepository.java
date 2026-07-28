package site.yesaido.cultivation_server.repository.cultivationmember;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.entity.cultivationmember.CultivationMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface CultivationMemberRepository extends CrudRepository<CultivationMember, Long> {
    boolean existsByCultivationIdAndUserId(Long cultivationId, Long userId);
    Optional<CultivationMember> findByCultivationIdAndUserId(Long cultivationId, Long userId);
    List<CultivationMember> findAllByCultivationId(Long cultivationId);
    void deleteAllByCultivationId(Long cultivationId);
}
