package site.yesaido.cultivation_server.rabbitmq.test.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.rabbitmq.test.service.RabbitSmokeService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Profile("rabbit-smoke")
@RestController
@RequestMapping("/internal/test/rabbit")
@RequiredArgsConstructor
public class RabbitSmokeController {

    private final RabbitSmokeService service;

    @PostMapping("/sensor-created")
    public ResponseEntity<Void> publish(
            @Valid @RequestBody RabbitSmokeRequest request
    ) {
        OffsetDateTime occurredAt =
                OffsetDateTime.now(ZoneOffset.UTC);

        service.publish(
                request.toThresholdEvent(occurredAt),
                request.toSensorEvents(occurredAt)
        );

        return ResponseEntity.accepted().build();
    }
}

