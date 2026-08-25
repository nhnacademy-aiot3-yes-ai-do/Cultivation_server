package site.yesaido.cultivation_server.cultivation.service;

import org.springframework.web.multipart.MultipartFile;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;

public interface CultivationPhotoService {
    PhotoUploadResponse uploadPhoto(Long cultivationId, Long userId, MultipartFile file);
    PhotoUploadListResponse getPhotos(Long cultivationId, Long userId);
    void deletePhoto(Long cultivationId, Long userId, Long photoId);
}
