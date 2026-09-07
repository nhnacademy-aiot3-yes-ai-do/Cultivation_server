package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;

import java.time.LocalDate;

/**
 * {@code EnvironmentComplianceController}의 OpenAPI 문서 정의.
 */
@Tag(name = "환경 준수율", description = "재배 환경이 설정 임계값 안에 머문 비율(현재 · 일별 · 기간) 조회")
public interface EnvironmentComplianceControllerDocs {

    @Operation(summary = "현재 환경 준수율 조회", description = "현재 기준 환경 준수율을 반환합니다.")
    ResponseEntity<EnvironmentComplianceResponse> get(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId);

    @Operation(summary = "일별 환경 준수율 조회", description = "지정한 날짜(미지정 시 오늘, Asia/Seoul)의 환경 준수율을 반환합니다.")
    ResponseEntity<EnvironmentComplianceResponse> getDaily(
            @Parameter(description = "재배 ID") Long cultivationId,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd, 생략 시 오늘)") LocalDate date,
            Long userId);

    @Operation(summary = "기간 환경 준수율 조회", description = "startDate~endDate 구간의 환경 준수율을 반환합니다.")
    ResponseEntity<EnvironmentComplianceResponse> getPeriod(
            @Parameter(description = "재배 ID") Long cultivationId,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)") LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)") LocalDate endDate,
            Long userId);
}
