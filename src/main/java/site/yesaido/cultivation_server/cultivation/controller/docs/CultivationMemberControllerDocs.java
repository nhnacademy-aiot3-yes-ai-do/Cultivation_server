package site.yesaido.cultivation_server.cultivation.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberAddRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberRoleUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.OwnerTransferRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.response.MemberListResponse;

/**
 * {@code CultivationMemberController}의 OpenAPI 문서 정의.
 */
@Tag(name = "재배 멤버", description = "재배 멤버 초대 · 목록 · 권한 변경 · 소유권 이전 · 제외")
public interface CultivationMemberControllerDocs {

    @Operation(summary = "멤버 초대", description = "재배에 멤버를 추가합니다. 소유자만 가능합니다.")
    @ApiResponse(responseCode = "201", description = "추가됨")
    ResponseEntity<Void> addMember(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId,
            MemberAddRequest request);

    @Operation(summary = "멤버 목록 조회", description = "재배 멤버 목록과 각 멤버의 역할을 반환합니다.")
    ResponseEntity<MemberListResponse> getMembers(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId,
            String role);

    @Operation(summary = "멤버 역할 변경", description = "재배 멤버의 역할을 변경합니다. 소유자만 가능합니다.")
    ResponseEntity<Void> updateMember(
            @Parameter(description = "재배 ID") Long cultivationId,
            @Parameter(description = "대상 사용자 ID") Long targetUserId,
            Long userId,
            MemberRoleUpdateRequest request);

    @Operation(summary = "소유권 이전", description = "재배 소유권을 다른 멤버에게 이전합니다. 현재 소유자만 가능합니다.")
    ResponseEntity<Void> transferOwnership(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId,
            OwnerTransferRequest request);

    @Operation(summary = "멤버 제외", description = "재배에서 멤버를 제외합니다. 소유자 또는 본인이 나가는 경우 가능합니다.")
    @ApiResponse(responseCode = "204", description = "제외됨")
    ResponseEntity<Void> removeMember(
            @Parameter(description = "재배 ID") Long cultivationId,
            @Parameter(description = "대상 사용자 ID") Long targetUserId,
            Long userId);
}
