package site.yesaido.cultivation_server.cultivation.dto.cultivationphoto;

import site.yesaido.common.storage.StorageType;

import java.time.LocalDateTime;

public record PhotoUploadResponse(
        Long photoId,
        String objectKey,
        String url,
        StorageType storageType,
        LocalDateTime uploadedAt
) {
}
