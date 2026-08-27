package site.yesaido.cultivation_server.rabbitmq.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ThresholdInfoEvent(

        @NotNull
        @Positive
        Long cultivationId,

        //빈 List 전달시 (ruleEngine에서 전부 삭제처리)
        @NotNull
        List<@Valid SensorRange> sensorRangeList,

        @NotNull
        OffsetDateTime occurredAt
        /*
            재배 환경 정보 추가/수정/삭제

             * 등록/전체 동기화: 현재 적용할 전체 SensorRange
             * 부분 수정: 변경된 SensorRange
             * 경작 종료: 빈 List, 해당 cultivationId 전체 임계값 삭제 및 생성 중단
        */
) {
        public static ThresholdInfoEvent from(
                Long cultivationId,
                List<EnvironmentSettingRequest> settings,
                Map<Long, SensorType> sensorTypeMap,
                OffsetDateTime occurredAt
        ) {
                List<SensorRange> ranges = settings.stream()
                        .map(setting -> {
                                SensorType sensorType =
                                        sensorTypeMap.get(setting.sensorTypeId());

                                return new SensorRange(
                                        sensorType.getType(),
                                        sensorType.getValueUnit(),
                                        setting.thresholdMin(),
                                        setting.thresholdMax()
                                );
                        })
                        .toList();

                return new ThresholdInfoEvent(
                        cultivationId,
                        ranges,
                        occurredAt
                );
        }
}
