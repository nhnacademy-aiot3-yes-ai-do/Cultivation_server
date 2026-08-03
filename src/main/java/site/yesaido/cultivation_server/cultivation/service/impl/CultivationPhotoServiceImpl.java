package site.yesaido.cultivation_server.cultivation.service.impl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.common.storage.ObjectKeyGenerator;
import site.yesaido.common.storage.StorageType;
import site.yesaido.common.storage.StorageUrlResolver;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.CultivationPhoto;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.exception.PhotoNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.cultivationphoto.CultivationPhotoRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationPhotoService;
import site.yesaido.cultivation_server.exception.client.BadRequestException;
import site.yesaido.cultivation_server.exception.client.UnsupportedMediaTypeException;
import site.yesaido.cultivation_server.exception.server.CustomServerException;
import site.yesaido.cultivation_server.exception.server.ServerErrorLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationPhotoServiceImpl implements CultivationPhotoService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final String DOMAIN = "cultivation-photo";

    private final CultivationPhotoRepository cultivationPhotoRepository;
    private final CultivationRepository cultivationRepository;
    private final MinioClient minioClient;
    private final StorageUrlResolver storageUrlResolver;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
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

        CultivationPhoto cultivationPhoto = CultivationPhoto.builder()
                .objectKey(objectKey)
                .storageType(StorageType.MINIO)
                .uploadedAt(LocalDateTime.now())
                .cultivation(cultivation)
                .build();
        cultivationPhotoRepository.save(cultivationPhoto);

        return toResponse(cultivationPhoto);
    }

    @Override
    public List<PhotoUploadResponse> getPhotos(Long cultivationId, Long userId) {
        cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        if (!cultivationRepository.isMember(cultivationId, userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        return cultivationPhotoRepository.findByCultivationIdOrderByUploadedAtDesc(cultivationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deletePhoto(Long cultivationId, Long userId, Long photoId) {
        cultivationPhotoRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));
        if (!cultivationRepository.isMember(cultivationId, userId)) {
            throw new CultivationAccessDeniedException(cultivationId);
        }

        CultivationPhoto photo = cultivationPhotoRepository.findById(photoId)
                .filter(p -> p.getCultivation().getId().equals(cultivationId))
                .orElseThrow(() -> new PhotoNotFoundException(cultivationId));

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(photo.getObjectKey())
                            .build()
            );
        } catch (Exception e) {
            throw new CustomServerException(
                    "사전 삭제에 실패했습니다.",
                    "MINIO 삭제 실패: photoId: " + photoId + ", objectKey: " + photo.getObjectKey() + ", cause: " + e.getMessage(),
                    ServerErrorLevel.ERROR_LEVEL
            );
        }

        cultivationPhotoRepository.delete(photo);
    }

    // Helper Method
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
