package site.yesaido.cultivation_server.sensor.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoResponse;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeAlreadyExistException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeNotFoundException;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SensorTypeManagementController.class)
public class SensorTypeManagementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SensorTypeService sensorTypeService;

    @Nested
    @DisplayName("센서 타입 등록 API")
    class RegisterSensorType {

        @Test
        @DisplayName("정상 요청시 201 created 반환")
        void registerSensorType() throws Exception {
            SensorTypeRequest sensorTypeRequest = new SensorTypeRequest("test-type", "test-unit");
            Long sensorTypeId = 1L;

            when(sensorTypeService.registerSensorType(any(SensorTypeRequest.class))).thenReturn(sensorTypeId);

            String s = objectMapper.writeValueAsString(sensorTypeRequest);

            mockMvc.perform(post("/api/v1/admin/sensor-types")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("http://localhost/api/v1/admin/sensor-types/1")));

            verify(sensorTypeService).registerSensorType(sensorTypeRequest);
        }

        @Test
        @DisplayName("이미 존재하는 센서 타입 등록시 409 충돌")
        void registerSensorTypeConflict() throws Exception {
            SensorTypeRequest sensorTypeRequest = new SensorTypeRequest("test-type", "test-unit");

            when(sensorTypeService.registerSensorType(any(SensorTypeRequest.class))).thenThrow(new SensorTypeAlreadyExistException(""));

            String s = objectMapper.writeValueAsString(sensorTypeRequest);

            mockMvc.perform(post("/api/v1/admin/sensor-types")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("valueUnit이 비어있으면 400 반환")
        void blankValueUnit() throws Exception {
            SensorTypeRequest invalid = new SensorTypeRequest("test-type", "");

            String s = objectMapper.writeValueAsString(invalid);

            mockMvc.perform(post("/api/v1/admin/sensor-types")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isBadRequest());

            verify(sensorTypeService, never()).registerSensorType(any());
        }
    }

    @Nested
    @DisplayName("센서 타입 수정 API")
    class UpdateSensorType {

        @Test
        @DisplayName("정상 요청시 204 no content 반환")
        void updateSensorType() throws Exception {
            SensorTypeRequest sensorTypeRequest = new SensorTypeRequest("test-type", "test-unit");
            long sensorTypeId = 1L;

            doNothing().when(sensorTypeService).updateSensorType(anyLong(), any(SensorTypeRequest.class));

            String s = objectMapper.writeValueAsString(sensorTypeRequest);

            mockMvc.perform(put("/api/v1/admin/sensor-types/{sensor-type-id}", sensorTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isNoContent());

            verify(sensorTypeService).updateSensorType(sensorTypeId, sensorTypeRequest);
        }

        @Test
        @DisplayName("존재하지 않는 센서 타입 수정시 404")
        void updateSensorTypeNotFound() throws Exception {
            SensorTypeRequest sensorTypeRequest = new SensorTypeRequest("test-type", "test-unit");
            long sensorTypeId = 1L;

            doThrow(new SensorTypeNotFoundException("")).when(sensorTypeService).updateSensorType(anyLong(), any(SensorTypeRequest.class));

            String s = objectMapper.writeValueAsString(sensorTypeRequest);

            mockMvc.perform(put("/api/v1/admin/sensor-types/{sensor-type-id}", sensorTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("valueUnit이 비어있으면 400 반환")
        void blankValueUnit() throws Exception {
            SensorTypeRequest invalid = new SensorTypeRequest("test-type", "");
            long sensorTypeId = 1L;

            String s = objectMapper.writeValueAsString(invalid);

            mockMvc.perform(put("/api/v1/admin/sensor-types/{sensor-type-id}", sensorTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s))
                    .andExpect(status().isBadRequest());

            verify(sensorTypeService, never()).registerSensorType(any());
        }
    }

    @Nested
    @DisplayName("센서 타입 삭제 API")
    class DeleteSensorType {

        @Test
        @DisplayName("정상 요청시 204 no content 반환")
        void deleteSensorType() throws Exception {
            long sensorTypeId = 1L;

            doNothing().when(sensorTypeService).deleteSensorType(anyLong());

            mockMvc.perform(delete("/api/v1/admin/sensor-types/{sensor-type-id}", sensorTypeId))
                    .andExpect(status().isNoContent());

            verify(sensorTypeService).deleteSensorType(sensorTypeId);
        }

        @Test
        @DisplayName("존재하지 않는 센서 타입 삭제시 404")
        void deleteSensorTypeNotFound() throws Exception {
            long sensorTypeId = 1L;

            doThrow(new SensorTypeNotFoundException("")).when(sensorTypeService).deleteSensorType(anyLong());

            mockMvc.perform(delete("/api/v1/admin/sensor-types/{sensor-type-id}", sensorTypeId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("센서 타입 조회 API")
    class GetSensorType {

        @Test
        @DisplayName("정상 요청시 200 ok 반환")
        void deleteSensorType() throws Exception {
            long sensorTypeId = 1L;
            SensorTypeInfoResponse response = new SensorTypeInfoResponse(sensorTypeId, "test-type", "test-unit");

            when(sensorTypeService.getSensorTypeById(anyLong())).thenReturn(response);

            String result = objectMapper.writeValueAsString(response);

            mockMvc.perform(get("/api/v1/admin/sensor-types/{sensor-type-id}", sensorTypeId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(result));

            verify(sensorTypeService).getSensorTypeById(sensorTypeId);
        }

        @Test
        @DisplayName("존재하지 않는 센서 타입 조회시 404")
        void deleteSensorTypeNotFound() throws Exception {
            long sensorTypeId = 1L;

            doThrow(new SensorTypeNotFoundException("")).when(sensorTypeService).getSensorTypeById(anyLong());

            mockMvc.perform(get("/api/v1/admin/sensor-types/{sensor-type-id}", sensorTypeId))
                    .andExpect(status().isNotFound());
        }
    }
}
