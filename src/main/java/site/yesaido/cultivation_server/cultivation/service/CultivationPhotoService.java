package site.yesaido.cultivation_server.cultivation.service;

import org.springframework.web.multipart.MultipartFile;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.DailyCultivationPhotoListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;

import java.time.LocalDate;

public interface CultivationPhotoService {
    PhotoUploadResponse uploadPhoto(Long cultivationId, Long userId, MultipartFile file);
    PhotoUploadListResponse getPhotos(Long cultivationId, Long userId);
    PhotoUploadListResponse getPhotos(Long cultivationId, Long userId, String role);
    void deletePhoto(Long cultivationId, Long userId, Long photoId);

    // ai-server 대상 날짜에 사진이 등록된 활성 경작지 사진 목록 조회
    DailyCultivationPhotoListResponse getDailyPhotos(LocalDate targetDate);
}
