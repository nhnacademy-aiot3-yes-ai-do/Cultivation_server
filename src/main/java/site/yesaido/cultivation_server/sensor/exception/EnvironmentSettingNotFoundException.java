package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.NotFoundException;

public class EnvironmentSettingNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "[EnvironmentSetting] 해당 경작지에 존재하지 않는 환경설정 요청입니다";

    public EnvironmentSettingNotFoundException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
