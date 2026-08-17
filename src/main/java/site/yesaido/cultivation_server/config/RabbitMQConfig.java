package site.yesaido.cultivation_server.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.*;

@Configuration
public class RabbitMQConfig {

    // Dead Letter 관련
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange());
    }

    // 센서 관련
    @Bean
    public TopicExchange sensorExchange() {
        return new TopicExchange(SENSOR_EXCHANGE);
    }

    @Bean
    public Queue dataSourceSensorInfoQueue() {
        return QueueBuilder
                .durable(DATA_SOURCE_SENSOR_INFO_QUEUE)
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Queue ruleEngineSensorInfoQueue() {
        return QueueBuilder
                .durable(RULE_ENGINE_SENSOR_INFO_QUEUE)
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Binding dataSourceSensorInfoBinding() {
        return BindingBuilder
                .bind(dataSourceSensorInfoQueue())
                .to(sensorExchange())
                .with(SENSOR_INFO_BINDING_KEY_PATTERN);
    }

    @Bean
    public Binding ruleEngineSensorInfoBinding() {
        return BindingBuilder
                .bind(ruleEngineSensorInfoQueue())
                .to(sensorExchange())
                .with(SENSOR_INFO_BINDING_KEY_PATTERN);
    }

    @Bean
    public Queue ruleEngineThresholdInfoQueue() {
        return QueueBuilder
                .durable(RULE_ENGINE_THRESHOLD_INFO_QUEUE)
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Binding ruleEngineThresholdInfoBinding() {
        return BindingBuilder
                .bind(ruleEngineThresholdInfoQueue())
                .to(sensorExchange())
                .with(THRESHOLD_INFO_BINDING_KEY_PATTERN);
    }

    @Bean
    public Queue dataSourceThresholdInfoQueue() {
        return QueueBuilder
                .durable(DATA_SOURCE_THRESHOLD_INFO_QUEUE)
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean Binding dataSourceThresholdInfoBinding() {
        return BindingBuilder
                .bind(dataSourceThresholdInfoQueue())
                .to(sensorExchange())
                .with(THRESHOLD_INFO_BINDING_KEY_PATTERN);
    }

    @Bean
    public Queue environmentComplianceRequestQueue() {
        return QueueBuilder
                .durable(ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE)
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Binding environmentComplianceRequestBinding() {
        return BindingBuilder
                .bind(environmentComplianceRequestQueue())
                .to(sensorExchange())
                .with(ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE);
    }

    // AI 관련
    @Bean
    public DirectExchange harvestExchange() {
        return new DirectExchange(HARVEST_EXCHANGE);
    }

    @Bean
    public Queue aiHarvestQueue() {
        return QueueBuilder
                .durable(AI_HARVEST_QUEUE)
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Binding aiHarvestBinding() {
        return BindingBuilder
                .bind(aiHarvestQueue())
                .to(harvestExchange())
                .with(AI_HARVEST_QUEUE);
    }

    // 알림 관련
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue notificationDoneQueue() {
        return QueueBuilder
                .durable(NOTIFICATION_DONE_QUEUE)
                .withArgument(DLX_KEY, DLX_NAME)
                .build();
    }

    @Bean
    public Binding notificationDoneBinding() {
        return BindingBuilder
                .bind(notificationDoneQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_DONE_QUEUE);
    }
}
