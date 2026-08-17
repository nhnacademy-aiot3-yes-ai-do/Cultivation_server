package site.yesaido.cultivation_server.sensor.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceThresholdRequest;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MushroomReferenceInfoResponseTest {

    @Test
    @DisplayName("from() - MushroomReference와 연결된 threshold 목록을 함께 변환한다")
    void fromMapsMushroomReferenceAndThresholds() {
        SensorType sensorType = new SensorType("TEMPERATURE", "C");
        ReflectionTestUtils.setField(sensorType, "id", 1L);
        MushroomReference mushroomReference = new MushroomReference("표고", "Shiitake", "Lentinula edodes");
        ReflectionTestUtils.setField(mushroomReference, "id", 5L);
        MushroomReferenceThresholdRequest dto = new MushroomReferenceThresholdRequest(
                null, null, new BigDecimal("18.0"), new BigDecimal("24.0"));
        MushroomReferenceThreshold.create(sensorType, mushroomReference, dto);

        MushroomReferenceInfoResponse response = MushroomReferenceInfoResponse.from(mushroomReference);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.mushroomNameKo()).isEqualTo("표고");
        assertThat(response.mushroomNameEn()).isEqualTo("Shiitake");
        assertThat(response.mushroomScientificName()).isEqualTo("Lentinula edodes");
        assertThat(response.thresholdInfoResponses()).hasSize(1);
        assertThat(response.thresholdInfoResponses().get(0).sensorType().id()).isEqualTo(1L);
        assertThat(response.createdAt()).isEqualTo(mushroomReference.getCreatedAt());
    }
}