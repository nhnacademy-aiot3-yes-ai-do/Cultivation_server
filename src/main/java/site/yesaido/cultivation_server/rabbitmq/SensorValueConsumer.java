package site.yesaido.cultivation_server.rabbitmq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;

import java.io.IOException;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.SENSOR_SENSOR_VALUE_QUEUE;

@RequiredArgsConstructor
@Component
public class SensorValueConsumer {

    @RabbitListener(queues = SENSOR_SENSOR_VALUE_QUEUE)
    public void process(
            SensorValueEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
