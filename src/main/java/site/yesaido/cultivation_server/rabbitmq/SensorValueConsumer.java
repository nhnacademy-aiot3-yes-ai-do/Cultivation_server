package site.yesaido.cultivation_server.rabbitmq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;
import site.yesaido.cultivation_server.sensor.service.InfluxService;

import java.io.IOException;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.SENSOR_SENSOR_VALUE_QUEUE;

@RequiredArgsConstructor
@Component
public class SensorValueConsumer {

    private final InfluxService influxService;

    @RabbitListener(queues = SENSOR_SENSOR_VALUE_QUEUE)
    public void process(
            SensorValueEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            influxService.save(event);
            channel.basicAck(deliveryTag, false); // 배치 작업시 true
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
