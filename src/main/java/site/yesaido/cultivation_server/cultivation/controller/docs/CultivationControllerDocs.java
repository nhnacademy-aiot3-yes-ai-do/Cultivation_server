package site.yesaido.cultivation_server.cultivation.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationCreateResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationDetailResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationFinishResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationHistoryPageResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationModeChangeResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationSummaryListResponse;

/**
 * {@code CultivationController}의 OpenAPI 문서 정의.
 */
@Tag(name = "재배", description = "재배 생성 · 조회 · 종료 · 삭제 · 이력 · 수확 모드 전환")
public interface CultivationControllerDocs {

    @Operation(summary = "재배 생성", description = "새 재배를 생성하고 요청자를 소유자로 등록합니다.")
    @ApiResponse(responseCode = "201", description = "생성됨")
    ResponseEntity<CultivationCreateResponse> create(Long userId, CultivationCreateRequest request);

    @Operation(summary = "내 재배 목록 조회", description = "요청자가 멤버로 속한 진행 중인 재배 목록을 반환합니다.")
    ResponseEntity<CultivationSummaryListResponse> getCultivations(Long userId);

    @Operation(summary = "재배 상세 조회", description = "재배 단건 상세를 반환합니다. 멤버 또는 시스템 관리자(ADMIN)만 조회할 수 있습니다.")
    ResponseEntity<CultivationDetailResponse> getCultivation(
            Long userId,
            String role,
            @Parameter(description = "재배 ID") Long cultivationId);

    @Operation(summary = "재배 종료", description = "진행 중인 재배를 종료 상태로 전환합니다.")
    ResponseEntity<CultivationFinishResponse> finish(
            Long userId,
            @Parameter(description = "재배 ID") Long cultivationId);

    @Operation(summary = "재배 이력 조회", description = "요청자의 종료된 재배 이력을 페이지 단위로 반환합니다.")
    ResponseEntity<CultivationHistoryPageResponse> getHistory(Long userId, @ParameterObject Pageable pageable);

    @Operation(summary = "재배 삭제", description = "재배를 삭제(소프트 삭제)합니다. 소유자 또는 관리자만 가능합니다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    ResponseEntity<Void> deleteCultivation(
            Long userId,
            String role,
            @Parameter(description = "재배 ID") Long cultivationId);

    @Operation(summary = "수확 모드 전환", description = "재배를 수확 모드로 전환합니다.")
    ResponseEntity<CultivationModeChangeResponse> switchToHarvestMode(
            Long userId,
            @Parameter(description = "재배 ID") Long cultivationId);
}
