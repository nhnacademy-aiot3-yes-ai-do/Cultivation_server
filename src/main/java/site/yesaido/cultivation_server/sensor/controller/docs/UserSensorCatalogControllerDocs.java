package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.response.ReusableCultivationSensorListResponse;

/**
 * {@code UserSensorCatalogController}의 OpenAPI 문서 정의.
 */
@Tag(name = "재배 센서", description = "재배별 센서 장치 등록 · 목록 · 삭제")
public interface UserSensorCatalogControllerDocs {

    @Operation(summary = "재사용 가능한 센서 조회",
            description = "요청자의 다른 재배에 등록돼 있어 새 재배에 재사용할 수 있는 센서 목록을 반환합니다. "
                    + "`exclude-cultivation-id`는 결과에서 제외할 재배입니다.")
    ResponseEntity<ReusableCultivationSensorListResponse> getReusableSensors(
            Long userId,
            @Parameter(description = "결과에서 제외할 재배 ID") long excludedCultivationId);
}
