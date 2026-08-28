package site.yesaido.cultivation_server.cultivation.repository.cultivationphoto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CultivationPhotoRepository extends JpaRepository<CultivationPhoto, Long> {
    List<CultivationPhoto> findByCultivationIdOrderByUploadedAtDesc(Long cultivationId);

    @Query("""
            SELECT p FROM CultivationPhoto p
            JOIN FETCH p.cultivation c
            WHERE c.cultivationStatus IN :activeStatuses
                 AND p.uploadedAt >= :startOfDay
                 AND p.uploadedAt < :endOfDay
           """)
    List<CultivationPhoto> findAllForDailyVisionAnalysis(
            @Param("activeStatuses") Collection<CultivationStatus> activeStatuses,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
            );
}
