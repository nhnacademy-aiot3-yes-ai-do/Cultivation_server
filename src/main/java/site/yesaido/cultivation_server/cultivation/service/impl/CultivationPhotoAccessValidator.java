package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.exception.PhotoNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.cultivationphoto.CultivationPhotoRepository;

@Component
@RequiredArgsConstructor
public class CultivationPhotoAccessValidator {
    private final CultivationRepository cultivationRepository;
    private final CultivationPhotoRepository cultivationPhotoRepository;

    @Transactional(readOnly = true)
    public String resolveObjectKey(Long cultivationId, Long userId, Long photoId) {
        cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));
        if (!cultivationRepository.isMember(cultivationId, userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        CultivationPhoto photo = cultivationPhotoRepository.findById(photoId)
                .filter(p -> p.getCultivation().getId().equals(cultivationId))
                .orElseThrow(() -> new PhotoNotFoundException(photoId));

        return photo.getObjectKey();
    }
}