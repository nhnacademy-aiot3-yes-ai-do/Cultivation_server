package site.yesaido.cultivation_server.rabbitmq.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoUpsertEvent;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;

import java.util.List;

@Profile("rabbit-smoke")
@Service
@RequiredArgsConstructor
public class RabbitSmokeService {

    private final ApplicationEventPublisher publisher;

    @Transactional
    public void publish(
            ThresholdInfoEvent threshold,
            List<SensorInfoUpsertEvent> sensors
    ) {
        // 논리적으로 임계값을 먼저 발행
        publisher.publishEvent(threshold);

        // 이후 타입별 센서 정보 발행
        sensors.forEach(publisher::publishEvent);
    }
}
