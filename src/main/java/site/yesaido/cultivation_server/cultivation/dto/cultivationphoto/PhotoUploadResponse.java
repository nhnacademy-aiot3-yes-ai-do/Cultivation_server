package site.yesaido.cultivation_server.cultivation.dto.cultivationphoto;

import site.yesaido.cultivation_server.cultivation.entity.cultivationphoto.StorageType;

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
