package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;

/**
 * {@code EnvironmentSettingController}의 OpenAPI 문서 정의.
 */
@Tag(name = "환경 설정", description = "재배 환경 센서 타입별 임계값(최소/최대) 설정")
public interface EnvironmentSettingControllerDocs {

    @Operation(summary = "환경 임계값 설정", description = "재배의 센서 타입별 임계값 범위를 갱신합니다.")
    @ApiResponse(responseCode = "204", description = "갱신됨")
    ResponseEntity<Void> update(
            Long userId,
            @Parameter(description = "재배 ID") long cultivationId,
            EnvironmentSettingRequest request);
}
