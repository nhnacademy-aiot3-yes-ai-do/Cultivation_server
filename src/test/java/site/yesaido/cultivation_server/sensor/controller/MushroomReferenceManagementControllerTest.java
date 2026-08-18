package site.yesaido.cultivation_server.sensor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceRequest;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceThresholdRequest;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoResponse;
import site.yesaido.cultivation_server.sensor.exception.MushroomReferenceAlreadyExistException;
import site.yesaido.cultivation_server.sensor.exception.MushroomReferenceNotFoundException;
import site.yesaido.cultivation_server.sensor.service.MushroomReferenceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MushroomReferenceManagementController.class)
class MushroomReferenceManagementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MushroomReferenceService mushroomReferenceService;

    @Nested
    @DisplayName("버섯 참조 등록 API")
    class RegisterMushroomReference {

        @Test
        @DisplayName("정상 요청시 201 created 반환")
        void registerMushroomReference() throws Exception {
            List<MushroomReferenceThresholdRequest> thresholdRequests = List.of(
                    new MushroomReferenceThresholdRequest(null, 1L, BigDecimal.valueOf(10.23), BigDecimal.valueOf(40.32)),
                    new MushroomReferenceThresholdRequest(null, 2L, BigDecimal.valueOf(10.32), BigDecimal.valueOf(40.23))
            );
            MushroomReferenceRequest request = new MushroomReferenceRequest("test-ko-name", "test-en-name", "test-scientific-name", thresholdRequests);
            Long mushroomReferenceId = 1L;

            when(mushroomReferenceService.registerMushroomReference(any(MushroomReferenceRequest.class))).thenReturn(mushroomReferenceId);

            String s = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/admin/mushroom-references")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("http://localhost/api/v1/admin/mushroom-references/1")));

            verify(mushroomReferenceService).registerMushroomReference(request);
        }

        @Test
        @DisplayName("이미 존재하는 버섯 참조 등록시 409 충돌")
        void registerMushroomReferenceConflict() throws Exception {
            List<MushroomReferenceThresholdRequest> thresholdRequests = List.of(
                    new MushroomReferenceThresholdRequest(null, 1L, BigDecimal.valueOf(10.23), BigDecimal.valueOf(40.32)),
                    new MushroomReferenceThresholdRequest(null, 2L, BigDecimal.valueOf(10.32), BigDecimal.valueOf(40.23))
            );
            MushroomReferenceRequest request = new MushroomReferenceRequest("test-ko-name", "test-en-name", "test-scientific-name", thresholdRequests);

            when(mushroomReferenceService.registerMushroomReference(any(MushroomReferenceRequest.class))).thenThrow(new MushroomReferenceAlreadyExistException(""));

            String s = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/admin/mushroom-references")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("valueUnit이 비어있으면 400 반환")
        void blankValueUnit() throws Exception {
            MushroomReferenceRequest request = new MushroomReferenceRequest("test-ko-name", "test-en-name", "test-scientific-name", null);

            String s = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/admin/mushroom-references")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());

            verify(mushroomReferenceService, never()).registerMushroomReference(any());
        }
    }

    @Nested
    @DisplayName("버섯 참조 수정 API")
    class UpdateMushroomReference {

        @Test
        @DisplayName("정상 요청시 204 no content 반환")
        void updateMushroomReference() throws Exception {
            List<MushroomReferenceThresholdRequest> thresholdRequests = List.of(
                    new MushroomReferenceThresholdRequest(1L, 1L, BigDecimal.valueOf(10.23), BigDecimal.valueOf(40.32)),
                    new MushroomReferenceThresholdRequest(2L, 2L, BigDecimal.valueOf(10.32), BigDecimal.valueOf(40.23))
            );
            MushroomReferenceRequest request = new MushroomReferenceRequest("test-ko-name", "test-en-name", "test-scientific-name", thresholdRequests);
            long mushroomReferenceId = 1L;

            doNothing().when(mushroomReferenceService).updateMushroomReference(anyLong(), any(MushroomReferenceRequest.class));

            String s = objectMapper.writeValueAsString(request);

            mockMvc.perform(put("/api/v1/admin/mushroom-references/{mushroom-references}", mushroomReferenceId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isNoContent());

            verify(mushroomReferenceService).updateMushroomReference(mushroomReferenceId, request);
        }

        @Test
        @DisplayName("존재하지 않는 버섯 참조 수정시 404")
        void updateMushroomReferenceNotFound() throws Exception {
            List<MushroomReferenceThresholdRequest> thresholdRequests = List.of(
                    new MushroomReferenceThresholdRequest(1L, 1L, BigDecimal.valueOf(10.23), BigDecimal.valueOf(40.32)),
                    new MushroomReferenceThresholdRequest(2L, 2L, BigDecimal.valueOf(10.32), BigDecimal.valueOf(40.23))
            );
            MushroomReferenceRequest request = new MushroomReferenceRequest("test-ko-name", "test-en-name", "test-scientific-name", thresholdRequests);
            Long mushroomReferenceId = 1L;

            doThrow(new MushroomReferenceNotFoundException("")).when(mushroomReferenceService).updateMushroomReference(anyLong(), any(MushroomReferenceRequest.class));

            String s = objectMapper.writeValueAsString(request);

            mockMvc.perform(put("/api/v1/admin/mushroom-references/{mushroom-references}", mushroomReferenceId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("valueUnit이 비어있으면 400 반환")
        void blankValueUnit() throws Exception {
            MushroomReferenceRequest request = new MushroomReferenceRequest("test-ko-name", "test-en-name", "test-scientific-name", null);
            Long mushroomReferenceId = 1L;

            String s = objectMapper.writeValueAsString(request);

            mockMvc.perform(put("/api/v1/admin/mushroom-references/{mushroom-references}", mushroomReferenceId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isBadRequest());

            verify(mushroomReferenceService, never()).updateMushroomReference(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("버섯 참조 삭제 API")
    class DeleteMushroomReference {

        @Test
        @DisplayName("정상 요청시 204 no content 반환")
        void deleteMushroomReference() throws Exception {
            long mushroomReferenceId = 1L;

            doNothing().when(mushroomReferenceService).deleteMushroomReference(anyLong());

            mockMvc.perform(delete("/api/v1/admin/mushroom-references/{mushroom-reference-id}", mushroomReferenceId))
                    .andExpect(status().isNoContent());

            verify(mushroomReferenceService).deleteMushroomReference(mushroomReferenceId);
        }

        @Test
        @DisplayName("존재하지 않는 센서 삭제시 404")
        void deleteMushroomReferenceNotFound() throws Exception {
            Long mushroomReferenceId = 1L;

            doThrow(new MushroomReferenceNotFoundException("")).when(mushroomReferenceService).deleteMushroomReference(anyLong());

            mockMvc.perform(delete("/api/v1/admin/mushroom-references/{mushroom-reference-id}", mushroomReferenceId))
                    .andExpect(status().isNotFound());

            verify(mushroomReferenceService).deleteMushroomReference(anyLong());
        }
    }

    @Nested
    @DisplayName("버섯 참조 조회 API")
    class GetMushroomReference {

        @Test
        @DisplayName("정상 요청시 200 ok 반환")
        void getMushroomReferenceById() throws Exception {
            long mushroomReferenceId = 1L;
            MushroomReferenceInfoResponse response = new MushroomReferenceInfoResponse(mushroomReferenceId,
                    "test-ko-name", "test-en-name", "test-scientific-name",
                    List.of(), LocalDateTime.now(ZoneId.of("Asia/Seoul")), null);

            when(mushroomReferenceService.getMushroomReferenceInfo(anyLong())).thenReturn(response);

            String result = objectMapper.writeValueAsString(response);

            mockMvc.perform(get("/api/v1/admin/mushroom-references/{mushroom-reference-id}", mushroomReferenceId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(result));

            verify(mushroomReferenceService).getMushroomReferenceInfo(mushroomReferenceId);
        }

        @Test
        @DisplayName("존재하지 않는 센서 타입 조회시 404")
        void getMushroomReferenceByIdNotFound() throws Exception {
            long mushroomReferenceId = 1L;

            when(mushroomReferenceService.getMushroomReferenceInfo(anyLong())).thenThrow(new MushroomReferenceNotFoundException(""));

            mockMvc.perform(get("/api/v1/admin/mushroom-references/{mushroom-reference-id}", mushroomReferenceId))
                    .andExpect(status().isNotFound());

            verify(mushroomReferenceService).getMushroomReferenceInfo(mushroomReferenceId);
        }
    }

    @Test
    @DisplayName("버섯 참조 모두 조회 API - 정상 요청시 200 ok 반환")
    void getAllMushroomReference() throws Exception {
        MushroomReferenceInfoResponse referenceInfoResponse1 = new MushroomReferenceInfoResponse(1L,
                "test-ko-name", "test-en-name", "test-scientific-name",
                List.of(), LocalDateTime.now(ZoneId.of("Asia/Seoul")), null);
        MushroomReferenceInfoResponse referenceInfoResponse2 = new MushroomReferenceInfoResponse(1L,
                "test-ko-name", "test-en-name", "test-scientific-name",
                List.of(), LocalDateTime.now(ZoneId.of("Asia/Seoul")), null);
        MushroomReferenceInfoListResponse response = new MushroomReferenceInfoListResponse(List.of(referenceInfoResponse1, referenceInfoResponse2));

        when(mushroomReferenceService.getAllMushroomReferenceInfoList()).thenReturn(response);

        String result = objectMapper.writeValueAsString(response);

        mockMvc.perform(get("/api/v1/admin/mushroom-references"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(result));

        verify(mushroomReferenceService).getAllMushroomReferenceInfoList();
    }

}
