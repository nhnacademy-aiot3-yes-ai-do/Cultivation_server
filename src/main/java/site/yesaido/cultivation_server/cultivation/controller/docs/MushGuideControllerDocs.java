package site.yesaido.cultivation_server.cultivation.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.cultivation.dto.ai.MushGuideResponse;

/**
 * {@code MushGuideController}의 OpenAPI 문서 정의.
 */
@Tag(name = "버섯 가이드", description = "버섯 재배 가이드 조회 (AI 서버 연동)")
public interface MushGuideControllerDocs {

    @Operation(summary = "버섯 가이드 조회", description = "버섯 종류별 재배 가이드(권장 환경, 레시피 등)를 반환합니다.")
    ResponseEntity<MushGuideResponse> getMushroomGuide(@Parameter(description = "버섯 ID") Long mushroomId);
}
