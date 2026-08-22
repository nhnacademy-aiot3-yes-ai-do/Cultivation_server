package site.yesaido.cultivation_server.cultivation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.common.storage.StorageType;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoRawContent;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationPhotoServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CultivationPhotoController.class)
class CultivationPhotoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CultivationPhotoServiceImpl cultivationPhotoService;

    @Test
    @DisplayName("사진 업로드 API - 정상 요청 시 201 Created 반환")
    void uploadPhotoSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "content".getBytes());
        PhotoUploadResponse response = new PhotoUploadResponse(
                200L, "cultivation-photo/100/uuid.jpg", "http://storage.example.com/test-bucket/uuid.jpg",
                StorageType.MINIO, LocalDateTime.now()
        );

        when(cultivationPhotoService.uploadPhoto(eq(cultivationId), eq(userId), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/cultivations/{cultivation-id}/photos", cultivationId)
                        .file(file)
                        .header("X-User-Id", userId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoId").value(200L))
                .andExpect(jsonPath("$.storageType").value(StorageType.MINIO.name()));
    }

    @Test
    @DisplayName("사진 목록 조회 API - 정상 요청 시 200 OK 반환")
    void getPhotosSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        PhotoUploadResponse response = new PhotoUploadResponse(
                200L, "cultivation-photo/100/uuid.jpg", "http://storage.example.com/test-bucket/uuid.jpg",
                StorageType.MINIO, LocalDateTime.now()
        );
        PhotoUploadListResponse responseList = new PhotoUploadListResponse(List.of(response));

        when(cultivationPhotoService.getPhotos(cultivationId, userId)).thenReturn(responseList);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/photos", cultivationId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUploadResponses[0].photoId").value(200L));
    }

    @Test
    @DisplayName("사진 삭제 API - 정상 요청 시 204 No Content 반환")
    void deletePhotoSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 200L;

        mockMvc.perform(delete("/api/v1/cultivations/{cultivation-id}/photos/{photo-id}", cultivationId, photoId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("사진 원본 조회 API - 정상 요청 시 200 OK와 이미지 바이트 반환")
    void getPhotoRawSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        Long photoId = 200L;
        byte[] content = "image-bytes".getBytes();
        PhotoRawContent raw = new PhotoRawContent(content, "image/jpeg");

        when(cultivationPhotoService.getPhotoRaw(cultivationId, userId, photoId)).thenReturn(raw);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/photos/{photo-id}/raw", cultivationId, photoId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(content));
    }
}
