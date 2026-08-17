package site.yesaido.cultivation_server.cultivation.repository.cultivationphoto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;

import java.util.List;

@Repository
public interface CultivationPhotoRepository extends JpaRepository<CultivationPhoto, Long> {
    List<CultivationPhoto> findByCultivationIdOrderByUploadedAtDesc(Long cultivationId);
}
