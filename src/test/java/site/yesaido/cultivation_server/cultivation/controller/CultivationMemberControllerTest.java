package site.yesaido.cultivation_server.cultivation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberAddRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberRoleUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.OwnerTransferRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.response.MemberResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.exception.CultivationMemberNotFoundException;
import site.yesaido.cultivation_server.cultivation.exception.InvalidMemberRoleException;
import site.yesaido.cultivation_server.cultivation.exception.InvalidOwnershipTransferException;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CultivationMemberController.class)
class CultivationMemberControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CultivationMemberService cultivationMemberService;

    private static final Long CULTIVATION_ID = 1L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long TARGET_ID = 200L;

    // ===== addMember =====

    @Test
    @DisplayName("멤버 추가 성공 - 201")
    void addMemberSuccess() throws Exception {
        MemberAddRequest request = new MemberAddRequest(TARGET_ID, MemberRole.MEMBER);

        doNothing().when(cultivationMemberService).addMember(eq(CULTIVATION_ID), eq(REQUESTER_ID), any(MemberAddRequest.class));

        mockMvc.perform(post("/api/cultivations/{cultivation-id}/members", CULTIVATION_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(cultivationMemberService).addMember(eq(CULTIVATION_ID), eq(REQUESTER_ID), any(MemberAddRequest.class));
    }

    @Test
    @DisplayName("멤버 추가 실패 - 요청자가 Owner가 아니면 403")
    void addMemberFailAccessDenied() throws Exception {
        MemberAddRequest request = new MemberAddRequest(TARGET_ID, MemberRole.MEMBER);

        doThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                .when(cultivationMemberService).addMember(eq(CULTIVATION_ID), eq(REQUESTER_ID), any(MemberAddRequest.class));

        mockMvc.perform(post("/api/cultivations/{cultivation-id}/members", CULTIVATION_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ===== getMembers =====

    @Test
    @DisplayName("멤버 목록 조회 성공 - 200")
    void getMembersSuccess() throws Exception {
        List<MemberResponse> responseList = List.of(
                new MemberResponse(1L, REQUESTER_ID, "owner", MemberRole.OWNER, LocalDateTime.now()),
                new MemberResponse(2L, TARGET_ID, "member", MemberRole.MEMBER, LocalDateTime.now())
        );

        when(cultivationMemberService.getMembers(CULTIVATION_ID, REQUESTER_ID)).thenReturn(responseList);

        mockMvc.perform(get("/api/cultivations/{cultivation-id}/members", CULTIVATION_ID)
                        .header("X-User-Id", REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    @DisplayName("멤버 목록 조회 실패 - 멤버가 아니면 403")
    void getMembersFailAccessDenied() throws Exception {
        when(cultivationMemberService.getMembers(CULTIVATION_ID, REQUESTER_ID))
                .thenThrow(new CultivationAccessDeniedException(CULTIVATION_ID));

        mockMvc.perform(get("/api/cultivations/{cultivation-id}/members", CULTIVATION_ID)
                        .header("X-User-Id", REQUESTER_ID))
                .andExpect(status().isForbidden());
    }

    // ===== updateMember (등급 변경) =====

    @Test
    @DisplayName("멤버 등급 변경 성공 - 200")
    void updateMemberSuccess() throws Exception {
        MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(MemberRole.MANAGER);

        doNothing().when(cultivationMemberService)
                .updateMember(eq(CULTIVATION_ID), eq(REQUESTER_ID), eq(TARGET_ID), any(MemberRoleUpdateRequest.class));

        mockMvc.perform(put("/api/cultivations/{cultivation-id}/members/{user-id}", CULTIVATION_ID, TARGET_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cultivationMemberService).updateMember(eq(CULTIVATION_ID), eq(REQUESTER_ID), eq(TARGET_ID), any(MemberRoleUpdateRequest.class));
    }

    @Test
    @DisplayName("멤버 등급 변경 실패 - OWNER로는 변경 불가(400)")
    void updateMemberFailInvalidRole() throws Exception {
        MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(MemberRole.OWNER);

        doThrow(new InvalidMemberRoleException())
                .when(cultivationMemberService)
                .updateMember(eq(CULTIVATION_ID), eq(REQUESTER_ID), eq(TARGET_ID), any(MemberRoleUpdateRequest.class));

        mockMvc.perform(put("/api/cultivations/{cultivation-id}/members/{user-id}", CULTIVATION_ID, TARGET_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버 등급 변경 실패 - 요청자가 Owner가 아니면 403")
    void updateMemberFailAccessDenied() throws Exception {
        MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(MemberRole.MANAGER);

        doThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                .when(cultivationMemberService)
                .updateMember(eq(CULTIVATION_ID), eq(REQUESTER_ID), eq(TARGET_ID), any(MemberRoleUpdateRequest.class));

        mockMvc.perform(put("/api/cultivations/{cultivation-id}/members/{user-id}", CULTIVATION_ID, TARGET_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("멤버 등급 변경 실패 - 대상 멤버가 없으면 404")
    void updateMemberFailNotFound() throws Exception {
        MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(MemberRole.MANAGER);

        doThrow(new CultivationMemberNotFoundException())
                .when(cultivationMemberService)
                .updateMember(eq(CULTIVATION_ID), eq(REQUESTER_ID), eq(TARGET_ID), any(MemberRoleUpdateRequest.class));

        mockMvc.perform(put("/api/cultivations/{cultivation-id}/members/{user-id}", CULTIVATION_ID, TARGET_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ===== transferOwnership (소유권 이전) =====

    @Test
    @DisplayName("소유권 이전 성공 - 200")
    void transferOwnershipSuccess() throws Exception {
        OwnerTransferRequest request = new OwnerTransferRequest(TARGET_ID);

        doNothing().when(cultivationMemberService).transferOwnership(CULTIVATION_ID, REQUESTER_ID, TARGET_ID);

        mockMvc.perform(put("/api/cultivations/{cultivation-id}/owner", CULTIVATION_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cultivationMemberService).transferOwnership(CULTIVATION_ID, REQUESTER_ID, TARGET_ID);
    }

    @Test
    @DisplayName("소유권 이전 실패 - 본인에게 이전 시 400")
    void transferOwnershipFailInvalidSelfTransfer() throws Exception {
        OwnerTransferRequest request = new OwnerTransferRequest(REQUESTER_ID);

        doThrow(new InvalidOwnershipTransferException())
                .when(cultivationMemberService).transferOwnership(CULTIVATION_ID, REQUESTER_ID, REQUESTER_ID);

        mockMvc.perform(put("/api/cultivations/{cultivation-id}/owner", CULTIVATION_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("소유권 이전 실패 - 요청자가 Owner가 아니면 403")
    void transferOwnershipFailAccessDenied() throws Exception {
        OwnerTransferRequest request = new OwnerTransferRequest(TARGET_ID);

        doThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                .when(cultivationMemberService).transferOwnership(CULTIVATION_ID, REQUESTER_ID, TARGET_ID);

        mockMvc.perform(put("/api/cultivations/{cultivation-id}/owner", CULTIVATION_ID)
                        .header("X-User-Id", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ===== removeMember =====

    @Test
    @DisplayName("멤버 제거 성공 - 204")
    void removeMemberSuccess() throws Exception {
        doNothing().when(cultivationMemberService).removeMember(CULTIVATION_ID, REQUESTER_ID, TARGET_ID);

        mockMvc.perform(delete("/api/cultivations/{cultivation-id}/members/{user-id}", CULTIVATION_ID, TARGET_ID)
                        .header("X-User-Id", REQUESTER_ID))
                .andExpect(status().isNoContent());

        verify(cultivationMemberService).removeMember(CULTIVATION_ID, REQUESTER_ID, TARGET_ID);
    }

    @Test
    @DisplayName("멤버 제거 실패 - 대상이 Owner면 403")
    void removeMemberFailAccessDenied() throws Exception {
        doThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                .when(cultivationMemberService).removeMember(CULTIVATION_ID, REQUESTER_ID, TARGET_ID);

        mockMvc.perform(delete("/api/cultivations/{cultivation-id}/members/{user-id}", CULTIVATION_ID, TARGET_ID)
                        .header("X-User-Id", REQUESTER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("멤버 제거 실패 - 대상 멤버가 없으면 404")
    void removeMemberFailNotFound() throws Exception {
        doThrow(new CultivationMemberNotFoundException())
                .when(cultivationMemberService).removeMember(CULTIVATION_ID, REQUESTER_ID, TARGET_ID);

        mockMvc.perform(delete("/api/cultivations/{cultivation-id}/members/{user-id}", CULTIVATION_ID, TARGET_ID)
                        .header("X-User-Id", REQUESTER_ID))
                .andExpect(status().isNotFound());
    }
}