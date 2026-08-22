package site.yesaido.cultivation_server.cultivation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.common.storage.StorageType;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.exception.PhotoNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.cultivationphoto.CultivationPhotoRepository;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationPhotoAccessValidator;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CultivationPhotoAccessValidatorTest {

    @Mock private CultivationRepository cultivationRepository;
    @Mock private CultivationPhotoRepository cultivationPhotoRepository;

    @InjectMocks
    private CultivationPhotoAccessValidator cultivationPhotoAccessValidator;

    @Test
    @DisplayName("objectKey 조회 성공")
    void resolveObjectKeySuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();
        CultivationPhoto photo = CultivationPhoto.builder()
                .objectKey("cultivation-photo/100/uuid.jpg")
                .storageType(StorageType.MINIO)
                .uploadedAt(LocalDateTime.now())
                .cultivation(cultivation)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        when(cultivationRepository.isMember(cultivationId, userId)).thenReturn(true);
        when(cultivationPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        String objectKey = cultivationPhotoAccessValidator.resolveObjectKey(cultivationId, userId, photoId);

        assertThat(objectKey).isEqualTo(photo.getObjectKey());
    }

    @Test
    @DisplayName("objectKey 조회 실패 - 존재하지 않는 재배")
    void resolveObjectKeyFailCultivationNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cultivationPhotoAccessValidator.resolveObjectKey(cultivationId, userId, photoId))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("objectKey 조회 실패 - 재배 멤버가 아닌 경우")
    void resolveObjectKeyFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(2L).name("버섯 농장").build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        when(cultivationRepository.isMember(cultivationId, userId)).thenReturn(false);

        assertThatThrownBy(() -> cultivationPhotoAccessValidator.resolveObjectKey(cultivationId, userId, photoId))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("objectKey 조회 실패 - 존재하지 않는 사진")
    void resolveObjectKeyFailPhotoNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        when(cultivationRepository.isMember(cultivationId, userId)).thenReturn(true);
        when(cultivationPhotoRepository.findById(photoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cultivationPhotoAccessValidator.resolveObjectKey(cultivationId, userId, photoId))
                .isInstanceOf(PhotoNotFoundException.class);
    }

    @Test
    @DisplayName("objectKey 조회 실패 - 다른 재배 소속 사진")
    void resolveObjectKeyFailWrongCultivation() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long otherCultivationId = 200L;
        Long photoId = 10L;
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();
        Cultivation otherCultivation = Cultivation.builder().id(otherCultivationId).userId(userId).name("다른 농장").build();
        CultivationPhoto photo = CultivationPhoto.builder()
                .objectKey("cultivation-photo/200/uuid.jpg")
                .storageType(StorageType.MINIO)
                .uploadedAt(LocalDateTime.now())
                .cultivation(otherCultivation)
                .build();

        when(cultivationRepository.findById(cultivationId)).thenReturn(Optional.of(cultivation));
        when(cultivationRepository.isMember(cultivationId, userId)).thenReturn(true);
        when(cultivationPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> cultivationPhotoAccessValidator.resolveObjectKey(cultivationId, userId, photoId))
                .isInstanceOf(PhotoNotFoundException.class);
    }
}