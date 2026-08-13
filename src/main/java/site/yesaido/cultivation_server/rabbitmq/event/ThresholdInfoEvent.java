package site.yesaido.cultivation_server.rabbitmq.event;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record ThresholdInfoEvent(
        Long cultivationId,

        //빈 List도 전달할 수 있음 (ruleEngine에서 처리)
        List<SensorRange> sensorRangeList,

        @NotNull
        OffsetDateTime occurredAt
        /*
            재배 환경 정보 추가/수정/삭제

             * 등록/전체 동기화: 현재 적용할 전체 SensorRange
             * 부분 수정: 변경된 SensorRange
             * 경작 종료: 빈 List, 해당 cultivationId 전체 임계값 삭제 및 생성 중단
        */
) {
}
