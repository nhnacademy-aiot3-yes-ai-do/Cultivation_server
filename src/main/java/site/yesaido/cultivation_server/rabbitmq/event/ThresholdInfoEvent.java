package site.yesaido.cultivation_server.rabbitmq.event;

import java.util.List;

public record ThresholdInfoEvent(
        Long cultivationId,

        //빈 List도 전달할 수 있음 (ruleEngine에서 처리)
        List<SensorRange> sensorRangeList

        /*
    재배 환경 정보 추가/수정/삭제

    추가 : 원소가 4개인 List<SensorRange>
    수정 : 원소가 1개인 List<SensorRange>
    삭제 : 원소가 0개인 List.of()
    */
) {
}
