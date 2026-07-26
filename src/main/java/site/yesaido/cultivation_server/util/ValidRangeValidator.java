package site.yesaido.cultivation_server.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import site.yesaido.cultivation_server.dto.environmentsetting.EnvironmentRange;

public class ValidRangeValidator implements ConstraintValidator<ValidRange, EnvironmentRange> {
    @Override
    public boolean isValid(EnvironmentRange environmentRange, ConstraintValidatorContext context) {
        if (environmentRange == null || environmentRange.min() == null || environmentRange.max() == null) {
            return true;
        }
        return environmentRange.min().compareTo(environmentRange.max()) <= 0;
    }
}
