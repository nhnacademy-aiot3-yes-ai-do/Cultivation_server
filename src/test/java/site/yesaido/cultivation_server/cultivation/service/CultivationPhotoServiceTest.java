package site.yesaido.cultivation_server.cultivation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.common.exception.client.BadRequestException;
import site.yesaido.common.exception.client.UnsupportedMediaTypeException;
import site.yesaido.common.exception.server.CustomServerException;
import site.yesaido.common.storage.MinioObjectStorage;
import site.yesaido.common.storage.MinioObjectStorageException;
import site.yesaido.common.storage.StorageType;
import site.yesaido.common.storage.StorageUrlResolver;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoRawContent;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.exception.PhotoNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivationphoto.CultivationPhotoRepository;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationAccessGuard;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationPhotoServiceImpl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CultivationPhotoServiceTest {

    @Mock private CultivationPhotoRepository cultivationPhotoRepository;
    @Mock private MinioObjectStorage minioObjectStorage;
    @Mock private CultivationAccessGuard cultivationAccessGuard;

    @InjectMocks
    private CultivationPhotoServiceImpl cultivationPhotoService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("사진 업로드 성공")
    void uploadPhotoSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        when(minioObjectStorage.presignedGetUrl(anyString(), any(Duration.class))).thenReturn("http://storage.example.com/test-bucket/objectKey");

        PhotoUploadResponse response = cultivationPhotoService.uploadPhoto(cultivationId, userId, file);

        assertThat(response).isNotNull();
        assertThat(response.storageType()).isEqualTo(StorageType.MINIO);
        assertThat(response.uri()).isEqualTo("http://storage.example.com/test-bucket/objectKey");

        verify(minioObjectStorage, times(1)).put(anyString(), any(MultipartFile.class));
        verify(cultivationPhotoRepository, times(1)).save(any(CultivationPhoto.class));
    }

    @Test
    @DisplayName("사진 업로드 실패 - 존재하지 않는 재배")
    void uploadPhotoFailCultivationNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        when(cultivationAccessGuard.requireMember(cultivationId, userId))
                .thenThrow(new CultivationNotFoundException(cultivationId));

        assertThatThrownBy(() -> cultivationPhotoService.uploadPhoto(cultivationId, userId, file))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("사진 업로드 실패 - 재배 멤버가 아닌 경우")
    void uploadPhotoFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "content".getBytes());

        when(cultivationAccessGuard.requireMember(cultivationId, userId))
                .thenThrow(new CultivationAccessDeniedException(cultivationId));

        assertThatThrownBy(() -> cultivationPhotoService.uploadPhoto(cultivationId, userId, file))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("사진 업로드 실패 - 빈 파일")
    void uploadPhotoFailEmptyFile() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MultipartFile emptyFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);

        assertThatThrownBy(() -> cultivationPhotoService.uploadPhoto(cultivationId, userId, emptyFile))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("사진 업로드 실패 - 지원하지 않는 파일 형식")
    void uploadPhotoFailUnsupportedContentType() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MultipartFile file = new MockMultipartFile("file", "photo.txt", "text/plain", "content".getBytes());
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);

        assertThatThrownBy(() -> cultivationPhotoService.uploadPhoto(cultivationId, userId, file))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    @DisplayName("사진 업로드 실패 - MinIO 업로드 실패")
    void uploadPhotoFailMinioUpload() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "content".getBytes());
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        doThrow(new MinioObjectStorageException("MinIO 업로드 실패", new RuntimeException("연결 실패")))
                .when(minioObjectStorage).put(anyString(), any(MultipartFile.class));

        assertThatThrownBy(() -> cultivationPhotoService.uploadPhoto(cultivationId, userId, file))
                .isInstanceOf(CustomServerException.class);

        verify(cultivationPhotoRepository, never()).save(any());
    }

    @Test
    @DisplayName("사진 업로드 실패 - DB 저장 실패 시 MinIO 보상 삭제")
    void uploadPhotoFailDbSaveCompensatesMinio() {
        Long userId = 1L;
        Long cultivationId = 100L;
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "content".getBytes());
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        when(cultivationPhotoRepository.save(any(CultivationPhoto.class))).thenThrow(new RuntimeException("DB 오류"));

        assertThatThrownBy(() -> cultivationPhotoService.uploadPhoto(cultivationId, userId, file))
                .isInstanceOf(CustomServerException.class);

        // 실제 트랜잭션 매니저가 롤백 시 해주는 일을 테스트에서 직접 흉내
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(minioObjectStorage, times(1)).removeQuietly(anyString());
    }

    @Test
    @DisplayName("사진 목록 조회 성공")
    void getPhotosSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();
        CultivationPhoto photo = CultivationPhoto.builder()
                .objectKey("cultivation-photo/100/uuid.jpg")
                .storageType(StorageType.MINIO)
                .uploadedAt(LocalDateTime.now())
                .cultivation(cultivation)
                .build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        when(cultivationPhotoRepository.findByCultivationIdOrderByUploadedAtDesc(cultivationId)).thenReturn(List.of(photo));
        when(minioObjectStorage.presignedGetUrl(eq(photo.getObjectKey()), any(Duration.class))).thenReturn("http://storage.example.com/test-bucket/photo.jpg");

        PhotoUploadListResponse response = cultivationPhotoService.getPhotos(cultivationId, userId);

        assertThat(response.photoUploadResponses()).hasSize(1);
        assertThat(response.photoUploadResponses().getFirst().objectKey()).isEqualTo(photo.getObjectKey());
    }

    @Test
    @DisplayName("사진 목록 조회 실패 - 존재하지 않는 재배")
    void getPhotosFailCultivationNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;

        when(cultivationAccessGuard.requireMember(cultivationId, userId))
                .thenThrow(new CultivationNotFoundException(cultivationId));

        assertThatThrownBy(() -> cultivationPhotoService.getPhotos(cultivationId, userId))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("사진 목록 조회 실패 - 재배 멤버가 아닌 경우")
    void getPhotosFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;

        when(cultivationAccessGuard.requireMember(cultivationId, userId))
                .thenThrow(new CultivationAccessDeniedException(cultivationId));

        assertThatThrownBy(() -> cultivationPhotoService.getPhotos(cultivationId, userId))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("사진 삭제 성공")
    void deletePhotoSuccess() {
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

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        when(cultivationPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        cultivationPhotoService.deletePhoto(cultivationId, userId, photoId);

        verify(cultivationPhotoRepository, times(1)).delete(photo);
    }

    @Test
    @DisplayName("사진 삭제 실패 - 존재하지 않는 사진")
    void deletePhotoFailPhotoNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        when(cultivationPhotoRepository.findById(photoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cultivationPhotoService.deletePhoto(cultivationId, userId, photoId))
                .isInstanceOf(PhotoNotFoundException.class);
    }

    @Test
    @DisplayName("사진 삭제 실패 - 다른 재배 소속 사진")
    void deletePhotoFailWrongCultivation() {
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

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);
        when(cultivationPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> cultivationPhotoService.deletePhoto(cultivationId, userId, photoId))
                .isInstanceOf(PhotoNotFoundException.class);
    }

    @Test
    @DisplayName("사진 업로드 실패 - 파일 크기 초과")
    void uploadPhotoFailFileTooLarge() {
        Long userId = 1L;
        Long cultivationId = 100L;
        byte[] oversized = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", oversized);
        Cultivation cultivation = Cultivation.builder().id(cultivationId).userId(userId).name("버섯 농장").build();

        when(cultivationAccessGuard.requireMember(cultivationId, userId)).thenReturn(cultivation);

        assertThatThrownBy(() -> cultivationPhotoService.uploadPhoto(cultivationId, userId, file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("사진 원본 조회 성공")
    void getPhotoRawSuccess() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;
        String objectKey = "cultivation-photo/100/uuid.jpg";
        byte[] content = "image-bytes".getBytes();

        when(cultivationAccessGuard.resolveObjectKey(cultivationId, userId, photoId)).thenReturn(objectKey);
        when(minioObjectStorage.get(objectKey))
                .thenReturn(new MinioObjectStorage.MinioObjectContent(content, "image/jpeg"));

        PhotoRawContent result = cultivationPhotoService.getPhotoRaw(cultivationId, userId, photoId);

        assertThat(result.bytes()).isEqualTo(content);
        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("사진 원본 조회 실패 - 존재하지 않는 재배")
    void getPhotoRawFailCultivationNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;

        when(cultivationAccessGuard.resolveObjectKey(cultivationId, userId, photoId))
                .thenThrow(new CultivationNotFoundException(cultivationId));

        assertThatThrownBy(() -> cultivationPhotoService.getPhotoRaw(cultivationId, userId, photoId))
                .isInstanceOf(CultivationNotFoundException.class);
    }

    @Test
    @DisplayName("사진 원본 조회 실패 - 재배 멤버가 아닌 경우")
    void getPhotoRawFailAccessDenied() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;

        when(cultivationAccessGuard.resolveObjectKey(cultivationId, userId, photoId))
                .thenThrow(new CultivationAccessDeniedException(cultivationId));

        assertThatThrownBy(() -> cultivationPhotoService.getPhotoRaw(cultivationId, userId, photoId))
                .isInstanceOf(CultivationAccessDeniedException.class);
    }

    @Test
    @DisplayName("사진 원본 조회 실패 - 존재하지 않는 사진(다른 재배 소속 포함)")
    void getPhotoRawFailPhotoNotFound() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;

        when(cultivationAccessGuard.resolveObjectKey(cultivationId, userId, photoId))
                .thenThrow(new PhotoNotFoundException(photoId));

        assertThatThrownBy(() -> cultivationPhotoService.getPhotoRaw(cultivationId, userId, photoId))
                .isInstanceOf(PhotoNotFoundException.class);
    }

    @Test
    @DisplayName("사진 원본 조회 실패 - MinIO 다운로드 실패")
    void getPhotoRawFailMinioDownload() {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 10L;
        String objectKey = "cultivation-photo/100/uuid.jpg";

        when(cultivationAccessGuard.resolveObjectKey(cultivationId, userId, photoId)).thenReturn(objectKey);
        when(minioObjectStorage.get(objectKey))
                .thenThrow(new MinioObjectStorageException("MinIO 다운로드 실패", new RuntimeException("연결 실패")));

        assertThatThrownBy(() -> cultivationPhotoService.getPhotoRaw(cultivationId, userId, photoId))
                .isInstanceOf(CustomServerException.class);
    }
}