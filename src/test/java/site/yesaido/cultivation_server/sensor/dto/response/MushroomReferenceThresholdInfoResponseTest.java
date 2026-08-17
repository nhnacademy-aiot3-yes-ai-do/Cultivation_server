package site.yesaido.cultivation_server.sensor.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceThresholdRequest;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MushroomReferenceThresholdInfoResponseTest {

    @Test
    @DisplayName("from() - threshold 집합을 응답 리스트로 변환한다")
    void fromMapsThresholdSet() {
        SensorType sensorType = new SensorType("TEMPERATURE", "C");
        ReflectionTestUtils.setField(sensorType, "id", 1L);
        MushroomReference mushroomReference = new MushroomReference("표고", "Shiitake", "Lentinula edodes");
        MushroomReferenceThresholdRequest dto = new MushroomReferenceThresholdRequest(
                null, null, new BigDecimal("18.0"), new BigDecimal("24.0"));
        MushroomReferenceThreshold threshold = MushroomReferenceThreshold.create(sensorType, mushroomReference, dto);

        List<MushroomReferenceThresholdInfoResponse> responses =
                MushroomReferenceThresholdInfoResponse.from(Set.of(threshold));

        assertThat(responses).hasSize(1);
        MushroomReferenceThresholdInfoResponse response = responses.get(0);
        assertThat(response.sensorType().id()).isEqualTo(1L);
        assertThat(response.sensorType().type()).isEqualTo("TEMPERATURE");
        assertThat(response.thresholdMin()).isEqualByComparingTo("18.0");
        assertThat(response.thresholdMax()).isEqualByComparingTo("24.0");
    }

    @Test
    @DisplayName("from() - 빈 집합이면 빈 리스트를 반환한다")
    void fromEmptySetReturnsEmptyList() {
        List<MushroomReferenceThresholdInfoResponse> responses =
                MushroomReferenceThresholdInfoResponse.from(Set.of());

        assertThat(responses).isEmpty();
    }
}