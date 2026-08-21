package site.yesaido.cultivation_server.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ControllerResponseLoggingAspectTest {

    @Test
    void logsNonEmptyResponseBodyAtInfo() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("SensorTypeController.getAll()");
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(
                new SensorTypeInfoListResponse(List.of(new SensorTypeInfoResponse(1L, "TEMPERATURE", "C")))));

        Logger logger = (Logger) LoggerFactory.getLogger(ControllerResponseLoggingAspect.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            Object result = new ControllerResponseLoggingAspect().logControllerResponse(joinPoint);

            assertThat(result).isInstanceOf(ResponseEntity.class);
            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("controller_response method=SensorTypeController.getAll()")
                            && message.contains("status=200")
                            && message.contains("TEMPERATURE"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void doesNotLogUnrelatedControllerResponseBody() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("CultivationController.getDetail()");
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok("must-not-be-logged"));

        Logger logger = (Logger) LoggerFactory.getLogger(ControllerResponseLoggingAspect.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            new ControllerResponseLoggingAspect().logControllerResponse(joinPoint);

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("body_type=String")
                            && !message.contains("must-not-be-logged"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(originalLevel);
        }
    }
}
