package site.yesaido.cultivation_server.cultivation.service.impl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.common.exception.client.BadRequestException;
import site.yesaido.common.exception.client.UnsupportedMediaTypeException;
import site.yesaido.common.exception.server.CustomServerException;
import site.yesaido.common.exception.server.ServerErrorLevel;
import site.yesaido.common.storage.ObjectKeyGenerator;
import site.yesaido.common.storage.StorageType;
import site.yesaido.common.storage.StorageUrlResolver;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.exception.PhotoNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.cultivationphoto.CultivationPhotoRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationPhotoService;

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

    private final CultivationPhotoRepository cultivationPhotoRepository;
    private final CultivationRepository cultivationRepository;
    private final MinioClient minioClient;
    private final StorageUrlResolver storageUrlResolver;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    @Transactional
    public PhotoUploadResponse uploadPhoto(Long cultivationId, Long userId, MultipartFile file) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        if (!cultivationRepository.isMember(cultivationId, userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("업로드할 사진 파일이 없습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new UnsupportedMediaTypeException("지원하지 않는 이미지 형식입니다: " + contentType);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("사진 파일 크기는 8MB를 초과할 수 없습니다.");
        }

        String objectKey = ObjectKeyGenerator.generate(DOMAIN, cultivationId, file.getOriginalFilename());

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new CustomServerException(
                    "사전 업로드에 실패했습니다.",
                    "MINIO 업로드 실패: cultivationId: " + cultivationId + ", objectKey: " + objectKey + ", cause: " + e.getMessage(),
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
                    compensateMinioUpload(objectKey);
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
                    "DB 저장 실패, MinIO 객체 보상 삭제 시도: cultivationId: " + cultivationId + ", objectKey: " + objectKey + ", cause: " + e.getMessage(),
                    ServerErrorLevel.ERROR_LEVEL
            );
        }

        return toResponse(cultivationPhoto);
    }

    @Override
    public PhotoUploadListResponse getPhotos(Long cultivationId, Long userId) {
        cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        if (!cultivationRepository.isMember(cultivationId, userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        List<PhotoUploadResponse> list = cultivationPhotoRepository.findByCultivationIdOrderByUploadedAtDesc(cultivationId).stream()
                .map(this::toResponse)
                .toList();

        return new PhotoUploadListResponse(list);
    }

    @Override
    @Transactional
    public void deletePhoto(Long cultivationId, Long userId, Long photoId) {
        cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));
        if (!cultivationRepository.isMember(cultivationId, userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        CultivationPhoto photo = cultivationPhotoRepository.findById(photoId)
                .filter(p -> p.getCultivation().getId().equals(cultivationId))
                .orElseThrow(() -> new PhotoNotFoundException(photoId));

        String objectKey = photo.getObjectKey();
        cultivationPhotoRepository.delete(photo);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(photo.getObjectKey())
                                    .build()
                    );
                } catch (Exception e) {
                    log.error("MinIO 객체 삭제 실패(고아 객체로 남음): photoId={}, objectKey={}, cause={}",
                            photoId, objectKey, e.getMessage());
                }
            }
        });
    }

    // Helper Method
    private void compensateMinioUpload(String objectKey) {
        // 방금 올린 Minio 파일을 지움. <- DB에 없는 파일이 스토리지에 남지 않게 정리함.
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception cleanupException) {
            log.error("MinIO 보상 삭제 실패(고아 객체로 남음): objectKey: {}, cause: {}", objectKey, cleanupException.getMessage());
        }
    }

    private PhotoUploadResponse toResponse(CultivationPhoto cultivationPhoto) {
        String url = storageUrlResolver.resolve(cultivationPhoto.getStorageType(), cultivationPhoto.getObjectKey());

        return new PhotoUploadResponse(
                cultivationPhoto.getId(),
                cultivationPhoto.getObjectKey(),
                url,
                cultivationPhoto.getStorageType(),
                cultivationPhoto.getUploadedAt()
        );
    }
}
