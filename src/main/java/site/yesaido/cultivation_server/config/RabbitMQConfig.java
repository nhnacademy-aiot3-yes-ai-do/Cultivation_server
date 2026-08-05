package site.yesaido.cultivation_server.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private static final String DLX_NAME = "yes-nhn.dlx";
    private static final String DLX_KEY = "x-dead-letter-exchange";

    // Dead Letter 관련
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("yes-nhn.dlq").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#");
    }

    // 센서 관련
    @Bean
    public TopicExchange sensorExchange() {
        return new TopicExchange("yes-nhn.sensor.exchange");
    }

    @Bean
    public Queue dataSourceSensorInfoQueue() {
        return QueueBuilder
                .durable("yes-nhn.data-source.sensor-info.queue")
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Queue ruleEngineSensorInfoQueue() {
        return QueueBuilder
                .durable("yes-nhn.rule-engine.sensor-info.queue")
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Binding dataSourceSensorInfoBinding() {
        return BindingBuilder
                .bind(dataSourceSensorInfoQueue())
                .to(sensorExchange())
                .with("yes-nhn.#.sensor-info.queue");
    }

    @Bean
    public Binding ruleEngineSensorInfoBinding() {
        return BindingBuilder
                .bind(ruleEngineSensorInfoQueue())
                .to(sensorExchange())
                .with("yes-nhn.#.sensor-info.queue");
    }

    @Bean
    public Queue ruleEngineThresholdInfoQueue() {
        return QueueBuilder
                .durable("yes-nhn.rule-engine.threshold-info.queue")
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Binding ruleEngineThresholdInfoBinding() {
        return BindingBuilder
                .bind(ruleEngineThresholdInfoQueue())
                .to(sensorExchange())
                .with("yes-nhn.rule-engine.threshold-info.queue");
    }

    // AI 관련
    @Bean
    public DirectExchange harvestExchange() {
        return new DirectExchange("yes-nhn.harvest.exchange");
    }

    @Bean
    public Queue aiHarvestQueue() {
        return QueueBuilder
                .durable("yes-nhn.ai.harvest.queue")
                .withArgument(DLX_KEY, DLX_NAME).build();
    }

    @Bean
    public Binding aiHarvestBinding() {
        return BindingBuilder
                .bind(aiHarvestQueue())
                .to(harvestExchange())
                .with("#");
    }

    // 알림 관련
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange("yes-nhn.notification.exchange");
    }

    @Bean
    public Queue notificationDoneQueue() {
        return QueueBuilder
                .durable("yes-nhn.notification.done.queue")
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Binding notificationDoneBinding() {
        return BindingBuilder
                .bind(notificationDoneQueue())
                .to(notificationExchange())
                .with("#");
    }
}
