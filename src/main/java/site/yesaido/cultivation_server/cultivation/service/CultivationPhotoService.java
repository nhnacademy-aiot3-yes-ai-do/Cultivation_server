package site.yesaido.cultivation_server.cultivation.service;

import org.springframework.web.multipart.MultipartFile;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;

import java.util.List;

public interface CultivationPhotoService {
    PhotoUploadResponse uploadPhoto(Long cultivationId, Long userId, MultipartFile file);
    List<PhotoUploadResponse> getPhotos(Long cultivationId, Long userId);
    void deletePhoto(Long cultivationId, Long userId, Long photoId);
}
