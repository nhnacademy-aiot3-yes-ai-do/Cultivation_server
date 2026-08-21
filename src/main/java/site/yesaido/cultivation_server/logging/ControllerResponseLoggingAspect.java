package site.yesaido.cultivation_server.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;

@Aspect
@Component
@Slf4j
public class ControllerResponseLoggingAspect {
    private static final int MAX_BODY_LENGTH = 2_000;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logControllerResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof ResponseEntity<?> response) {
            log.info(
                    "controller_response method={} status={} {}",
                    joinPoint.getSignature().toShortString(),
                    response.getStatusCode().value(),
                    summarize(response.getBody())
            );
        }

        return result;
    }

    private String summarize(Object body) {
        if (body == null) {
            return "body_null=true";
        }
        if (body instanceof SensorTypeInfoListResponse || body instanceof MushroomReferenceInfoListResponse) {
            return "body=" + sanitizeAndTruncate(String.valueOf(body));
        }

        return "body_type=" + body.getClass().getSimpleName() + " body_null=false";
    }

    private String sanitizeAndTruncate(String value) {
        String sanitized = value
                .replace("\r", "\\r")
                .replace("\n", "\\n");

        return sanitized.length() <= MAX_BODY_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
    }
}
