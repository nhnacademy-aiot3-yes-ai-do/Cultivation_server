package site.yesaido.cultivation_server.cultivation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberAddRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberRoleUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.OwnerTransferRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.response.MemberListResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;

@RestController
@RequestMapping("/api/v1/cultivations/{cultivation-id}")
@RequiredArgsConstructor
public class CultivationMemberController {
    private final CultivationMemberService cultivationMemberService;

    @PostMapping("/members")
    public ResponseEntity<Void> addMember(@PathVariable("cultivation-id") Long cultivationId,
                                           @RequestHeader("X-User-Id") Long userId,
                                           @Valid @RequestBody MemberAddRequest request) {
        cultivationMemberService.addMember(cultivationId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/members")
    public ResponseEntity<MemberListResponse> getMembers(@PathVariable("cultivation-id") Long cultivationId,
                                                         @RequestHeader("X-User-Id") Long userId,
                                                         @RequestHeader(value = "X-User-Role", required = false) String role) {
        MemberListResponse response = cultivationMemberService.getMembers(cultivationId, userId, role);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/members/{user-id}")
    public ResponseEntity<Void> updateMember(@PathVariable("cultivation-id") Long cultivationId,
                                              @PathVariable("user-id") Long targetUserId,
                                              @RequestHeader("X-User-Id") Long userId,
                                              @Valid @RequestBody MemberRoleUpdateRequest request) {
        cultivationMemberService.updateMember(cultivationId, userId, targetUserId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/owner")
    public ResponseEntity<Void> transferOwnership(@PathVariable("cultivation-id") Long cultivationId,
                                                  @RequestHeader("X-User-Id") Long userId,
                                                  @Valid @RequestBody OwnerTransferRequest request) {
        cultivationMemberService.transferOwnership(cultivationId, userId, request.userId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/members/{user-id}")
    public ResponseEntity<Void> removeMember(@PathVariable("cultivation-id") Long cultivationId,
                                              @PathVariable("user-id") Long targetUserId,
                                              @RequestHeader("X-User-Id") Long userId) {
        cultivationMemberService.removeMember(cultivationId, userId, targetUserId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
