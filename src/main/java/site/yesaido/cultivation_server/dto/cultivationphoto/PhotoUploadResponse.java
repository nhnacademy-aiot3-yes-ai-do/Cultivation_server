package site.yesaido.cultivation_server.dto.cultivationphoto;

import site.yesaido.cultivation_server.entity.cultivationphoto.StorageType;

import java.time.LocalDate;

public record PhotoUploadResponse(
        Long photoId,
        String objectKey,
        StorageType storageType,
        Integer growthStage,
        LocalDate expectedHarvestDate,
        String improvement
) {
}
