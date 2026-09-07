package site.yesaido.cultivation_server.cultivation.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;

/**
 * {@code CultivationPhotoController}의 OpenAPI 문서 정의.
 */
@Tag(name = "재배 사진", description = "재배 사진 업로드 · 목록 · 삭제")
public interface CultivationPhotoControllerDocs {

    @Operation(summary = "사진 업로드", description = "재배에 사진 파일을 업로드합니다. (multipart/form-data)")
    @ApiResponse(responseCode = "201", description = "업로드됨")
    ResponseEntity<PhotoUploadResponse> uploadPhoto(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId,
            MultipartFile file);

    @Operation(summary = "사진 목록 조회", description = "재배에 업로드된 사진 목록을 반환합니다.")
    ResponseEntity<PhotoUploadListResponse> getPhotos(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId,
            String role);

    @Operation(summary = "사진 삭제", description = "재배 사진을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    ResponseEntity<Void> deletePhoto(
            @Parameter(description = "재배 ID") Long cultivationId,
            @Parameter(description = "사진 ID") Long photoId,
            Long userId);
}
