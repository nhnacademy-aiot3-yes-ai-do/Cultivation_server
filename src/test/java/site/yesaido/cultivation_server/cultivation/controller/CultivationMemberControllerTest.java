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
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.response.MemberResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("멤버 추가 성공 - 201")
    void addMemberSuccess() throws Exception {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        MemberAddRequest request = new MemberAddRequest(200L, MemberRole.MEMBER);

        doNothing().when(cultivationMemberService).addMember(eq(cultivationId), eq(requesterId), any(MemberAddRequest.class));

        mockMvc.perform(post("/api/cultivations/{cultivation-id}/members", cultivationId)
                .header("X-User-ID", requesterId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멤버 목록 조회 성공 - 200")
    void getMembersSuccess() throws Exception {
        Long cultivationId = 1L;
        Long requesterId = 100L;
        List<MemberResponse> responseList = List.of(
                new MemberResponse(1L, 100L, "owner", MemberRole.OWNER, LocalDateTime.now()),
                new MemberResponse(1L, 200L, "member",MemberRole.MEMBER, LocalDateTime.now())
        );

        when(cultivationMemberService.getMembers(cultivationId, requesterId)).thenReturn(responseList);

        mockMvc.perform(get("/api/cultivations/{cultivation-id}/members", cultivationId)
                .header("X-User-Id", requesterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

//    @Test
//    @DisplayName("멤버 권한 수정 성공")
//    void
}
