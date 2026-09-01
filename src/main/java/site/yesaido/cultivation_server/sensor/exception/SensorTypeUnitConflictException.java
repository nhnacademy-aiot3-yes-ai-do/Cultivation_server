package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.ConflictException;

// 원래 같은 센서타입에 다른 유닛단위인 경우 허락하는데 온도의 도씨 화씨같이 프론트에서 한번 입력 받을시 preparationService같이 하나 입력받으면
// 나머지 하나도 같이 변환해서 반환해야하는 유닛단위가 있을때 두개의 입력이 들어와서 서로 변환해서 같아야하는데 각기 다른 값이 저장 요청들어왔을때
// 백엔드에서 막을 때 사용
public class SensorTypeUnitConflictException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "[EnvironmentSetting] 임계값 설정에서 동시에 다른값을 받으면 안되는 최소 두개이상의 유닛단위 묶음입니다.";

    public SensorTypeUnitConflictException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
