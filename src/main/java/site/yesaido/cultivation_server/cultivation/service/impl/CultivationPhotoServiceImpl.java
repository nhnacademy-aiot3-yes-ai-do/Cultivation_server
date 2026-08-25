package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.common.exception.client.BadRequestException;
import site.yesaido.common.exception.client.UnsupportedMediaTypeException;
import site.yesaido.common.exception.server.CustomServerException;
import site.yesaido.common.exception.server.ServerErrorLevel;
import site.yesaido.common.storage.*;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoRawContent;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;
import site.yesaido.cultivation_server.cultivation.exception.PhotoNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivationphoto.CultivationPhotoRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationPhotoService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationPhotoServiceImpl implements CultivationPhotoService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 8L * 1024 * 1024;
    private static final String DOMAIN = "cultivation-photo";
    private static final String OBJECT_KEY_LOG_SEGMENT = ", objectKey: ";
    private static final String CAUSE_LOG_SEGMENT = ", cause: ";
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(30);

    private final CultivationPhotoRepository cultivationPhotoRepository;
    private final MinioObjectStorage minioObjectStorage;
    private final CultivationAccessGuard cultivationAccessGuard;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    @Transactional
    public PhotoUploadResponse uploadPhoto(Long cultivationId, Long userId, MultipartFile file) {
        Cultivation cultivation = cultivationAccessGuard.requireMember(cultivationId, userId);

        if (ImageFileValidator.isEmpty(file)) {
            throw new BadRequestException("업로드할 사진 파일이 없습니다.");
        }

        if (!ImageFileValidator.isAllowedContentType(file, ALLOWED_CONTENT_TYPES)) {
            throw new UnsupportedMediaTypeException("지원하지 않는 이미지 형식입니다: " + file.getContentType());
        }

        if (ImageFileValidator.exceedsMaxSize(file, MAX_FILE_SIZE)) {
            throw new BadRequestException("사진 파일 크기는 8MB를 초과할 수 없습니다.");
        }

        String objectKey = ObjectKeyGenerator.generate(DOMAIN, cultivationId, file.getOriginalFilename());

        try {
            minioObjectStorage.put(objectKey, file);
        } catch (MinioObjectStorageException e) {
            throw new CustomServerException(
                    "사전 업로드에 실패했습니다.",
                    "MINIO 업로드 실패: cultivationId: " + cultivationId + OBJECT_KEY_LOG_SEGMENT + objectKey + CAUSE_LOG_SEGMENT + e.getMessage(),
                    ServerErrorLevel.ERROR_LEVEL
            );
        }

        // 지금 진행중인 트랜잭션에, 끝날 때 실행할 콜백 하나 등록
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // 트랜잭션이 커밋 또는 롤백이 완전히 끝났을 떄 Spring 한 번 호출함.
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    // 롤백이 된 경우 compensateMinioUpload를 실행시킴.
                    minioObjectStorage.removeQuietly(objectKey);
                }
            }
        });

        CultivationPhoto cultivationPhoto = CultivationPhoto.builder()
                .objectKey(objectKey)
                .storageType(StorageType.MINIO)
                .uploadedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .cultivation(cultivation)
                .build();
        try {
            cultivationPhotoRepository.save(cultivationPhoto);
        } catch (Exception e) {
            throw new CustomServerException(
                    "사진 업로드에 실패했습니다.",
                    "DB 저장 실패, MinIO 객체 보상 삭제 시도: cultivationId: " + cultivationId + OBJECT_KEY_LOG_SEGMENT + objectKey + CAUSE_LOG_SEGMENT + e.getMessage(),
                    ServerErrorLevel.ERROR_LEVEL
            );
        }

        return toResponse(cultivationPhoto);
    }

    @Override
    public PhotoUploadListResponse getPhotos(Long cultivationId, Long userId) {
        cultivationAccessGuard.requireMember(cultivationId, userId);

        List<PhotoUploadResponse> list = cultivationPhotoRepository.findByCultivationIdOrderByUploadedAtDesc(cultivationId).stream()
                .map(this::toResponse)
                .toList();

        return new PhotoUploadListResponse(list);
    }

    @Override
    @Transactional
    public void deletePhoto(Long cultivationId, Long userId, Long photoId) {
        cultivationAccessGuard.requireMember(cultivationId, userId);

        CultivationPhoto photo = cultivationPhotoRepository.findById(photoId)
                .filter(p -> p.getCultivation().getId().equals(cultivationId))
                .orElseThrow(() -> new PhotoNotFoundException(photoId));

        String objectKey = photo.getObjectKey();
        cultivationPhotoRepository.delete(photo);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                minioObjectStorage.removeQuietly(objectKey);
            }
        });
    }

    @Override
    // getPhotoRaw 메서드가 실행되는 동안 진행 중이던 트랜잭션이 있어도 잠시 중단시켜서
    // 클래스 레벨 기본값과 무관하게 이 메서드만은 절대 DB 커넥션을 쥔 채로 실행되지 않도록 강제함.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PhotoRawContent getPhotoRaw(Long cultivationId, Long userId, Long photoId) {
        String objectKey = cultivationAccessGuard.resolveObjectKey(cultivationId, userId, photoId);

        try {
            MinioObjectStorage.MinioObjectContent content = minioObjectStorage.get(objectKey);
            return new PhotoRawContent(content.bytes(), content.contentType());
        } catch (MinioObjectStorageException e) {
            throw new CustomServerException(
                    "사진을 불러오는데 실패했습니다.",
                    "MINIO 다운로드 실패: photoId: " + photoId + OBJECT_KEY_LOG_SEGMENT + objectKey + CAUSE_LOG_SEGMENT + e.getMessage(),
                    ServerErrorLevel.WARN_LEVEL
            );
        }
    }

    // Helper Method
    private PhotoUploadResponse toResponse(CultivationPhoto cultivationPhoto) {
        String objectKey = cultivationPhoto.getObjectKey();
        String presignedUrl;
        try {
            presignedUrl = minioObjectStorage.presignedGetUrl(objectKey, PRESIGNED_URL_TTL);
        } catch (MinioObjectStorageException e) {
            throw new CustomServerException(
                    "사진 URL 발급 실패했습니다.",
                    "MINIO presigned URL 발급 실패" + OBJECT_KEY_LOG_SEGMENT + objectKey + CAUSE_LOG_SEGMENT + e.getMessage(),
                    ServerErrorLevel.WARN_LEVEL
            );
        }
        return new PhotoUploadResponse(
                cultivationPhoto.getId(),
                objectKey,
                presignedUrl,
                cultivationPhoto.getStorageType(),
                cultivationPhoto.getUploadedAt()
        );
    }
}
