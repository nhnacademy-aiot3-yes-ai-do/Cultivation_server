package site.yesaido.cultivation_server.cultivation.dto.cultivationphoto;

import java.util.List;

public record PhotoUploadListResponse(
        List<PhotoUploadResponse> photoUploadResponses
) {
}
