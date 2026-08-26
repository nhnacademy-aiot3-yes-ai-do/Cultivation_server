package site.yesaido.cultivation_server.sensor.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UniqueSensorTypeIdsValidator implements ConstraintValidator<UniqueSensorTypeIds, List<EnvironmentSettingRequest>> {

    @Override
    public boolean isValid(List<EnvironmentSettingRequest> settings, ConstraintValidatorContext constraintValidatorContext) {
        if (settings == null) {
            return true; // null/빈 목록은 @NotEmpty가 처리
        }

        Set<Long> sensorTypeIds = new HashSet<>();

        return settings.stream()
                .filter(Objects::nonNull)
                .map(EnvironmentSettingRequest::sensorTypeId)
                .filter(Objects::nonNull)
                .allMatch(sensorTypeIds::add); // 계속 sensorTypeId 저장하다 중복 발견하면 false
    }
}