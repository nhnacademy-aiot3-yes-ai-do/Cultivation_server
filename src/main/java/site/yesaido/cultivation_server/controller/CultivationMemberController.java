package site.yesaido.cultivation_server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.dto.cultivationmember.request.MemberAddRequest;
import site.yesaido.cultivation_server.dto.cultivationmember.request.MemberRoleUpdateRequest;
import site.yesaido.cultivation_server.dto.cultivationmember.response.MemberResponse;
import site.yesaido.cultivation_server.service.CultivationMemberService;

import java.util.List;

@RestController
@RequestMapping("/api/cultivations/{cultivation-id}/members")
@RequiredArgsConstructor
public class CultivationMemberController {
    private final CultivationMemberService cultivationMemberService;

    @PostMapping
    public ResponseEntity<Void> addMember(@PathVariable("cultivation-id") Long cultivationId,
                                           @RequestHeader("X-User-Id") Long userId,
                                           @Valid @RequestBody MemberAddRequest request) {
        cultivationMemberService.addMember(cultivationId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable("cultivation-id") Long cultivationId,
                                                             @RequestHeader("X-User-Id") Long userId) {
        List<MemberResponse> response = cultivationMemberService.getMembers(cultivationId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{user-id}")
    public ResponseEntity<Void> updateMember(@PathVariable("cultivation-id") Long cultivationId,
                                              @PathVariable("user-id") Long targetUserId,
                                              @RequestHeader("X-User-Id") Long userId,
                                              @Valid @RequestBody MemberRoleUpdateRequest request) {
        cultivationMemberService.updateMember(cultivationId, userId, targetUserId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{user-id}")
    public ResponseEntity<Void> removeMember(@PathVariable("cultivation-id") Long cultivationId,
                                              @PathVariable("user-id") Long targetUserId,
                                              @RequestHeader("X-User-Id") Long userId) {
        cultivationMemberService.removeMember(cultivationId, userId, targetUserId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
