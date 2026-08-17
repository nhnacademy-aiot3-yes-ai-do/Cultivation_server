package site.yesaido.cultivation_server.sensor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.sensor.service.EnvironmentComplianceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnvironmentComplianceController.class)
class EnvironmentComplianceControllerTest {

    private static final Long CULTIVATION_ID = 100L;
    private static final Long USER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    EnvironmentComplianceService environmentComplianceService;

    @MockitoBean
    CultivationMemberService cultivationMemberService;

    @Test
    @DisplayName("전체 기간 환경 유지율 조회 성공 시 200 OK와 결과를 반환한다")
    void getComplianceSuccess() throws Exception {
        EnvironmentComplianceResponse response = new EnvironmentComplianceResponse(
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(95), BigDecimal.valueOf(80));
        given(environmentComplianceService.getCompliance(CULTIVATION_ID)).willReturn(response);

        mockMvc.perform(get("/api/cultivations/{cultivation-id}/environment-compliance", CULTIVATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(cultivationMemberService).should().existCultivationMember(CULTIVATION_ID, USER_ID);
        then(environmentComplianceService).should().getCompliance(CULTIVATION_ID);
    }

    @Test
    @DisplayName("재배 멤버가 아니면 환경 유지율 조회 없이 403을 반환한다")
    void getComplianceFailsWhenNotMember() throws Exception {
        willThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                .given(cultivationMemberService).existCultivationMember(CULTIVATION_ID, USER_ID);

        mockMvc.perform(get("/api/cultivations/{cultivation-id}/environment-compliance", CULTIVATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isForbidden());

        then(environmentComplianceService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일별 환경 유지율 조회 - date 파라미터를 지정하면 해당 날짜로 조회한다")
    void getDailyComplianceWithExplicitDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 1);
        EnvironmentComplianceResponse response = new EnvironmentComplianceResponse(
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(95), BigDecimal.valueOf(80));
        given(environmentComplianceService.getDailyCompliance(CULTIVATION_ID, date)).willReturn(response);

        mockMvc.perform(get("/api/cultivations/{cultivation-id}/environment-compliance/daily", CULTIVATION_ID)
                        .param("date", "2026-08-01")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(environmentComplianceService).should().getDailyCompliance(CULTIVATION_ID, date);
    }

    @Test
    @DisplayName("일별 환경 유지율 조회 - date 파라미터가 없으면 한국시간 기준 오늘 날짜로 조회한다")
    void getDailyComplianceWithoutDateUsesToday() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        EnvironmentComplianceResponse response = new EnvironmentComplianceResponse(
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(95), BigDecimal.valueOf(80));
        given(environmentComplianceService.getDailyCompliance(CULTIVATION_ID, today)).willReturn(response);

        mockMvc.perform(get("/api/cultivations/{cultivation-id}/environment-compliance/daily", CULTIVATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk());

        then(environmentComplianceService).should().getDailyCompliance(CULTIVATION_ID, today);
    }

    @Test
    @DisplayName("기간별 환경 유지율 조회 성공 시 200 OK와 결과를 반환한다")
    void getPeriodComplianceSuccess() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 17);
        EnvironmentComplianceResponse response = new EnvironmentComplianceResponse(
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(95), BigDecimal.valueOf(80));
        given(environmentComplianceService.getComplianceForPeriod(CULTIVATION_ID, startDate, endDate)).willReturn(response);

        mockMvc.perform(get("/api/cultivations/{cultivation-id}/environment-compliance/period", CULTIVATION_ID)
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-17")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(environmentComplianceService).should().getComplianceForPeriod(CULTIVATION_ID, startDate, endDate);
    }
}