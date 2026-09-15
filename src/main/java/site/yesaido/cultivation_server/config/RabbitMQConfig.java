package site.yesaido.cultivation_server.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import site.yesaido.common.rabbitmq.DeadLetterQueues;
import site.yesaido.common.rabbitmq.DeadLetterTopologyConfiguration;
import site.yesaido.common.rabbitmq.RabbitDeadLetterProperties;

import static site.yesaido.cultivation_server.rabbitmq.RabbitMQConstants.*;

@Configuration
@Import(DeadLetterTopologyConfiguration.class)
public class RabbitMQConfig {
    // 센서 관련
    @Bean
    public TopicExchange sensorExchange() {
        return new TopicExchange(SENSOR_EXCHANGE);
    }

    @Bean
    public Queue dataSourceSensorInfoQueue(RabbitDeadLetterProperties dlProps) {
        return DeadLetterQueues.durableWithDeadLetter(DATA_SOURCE_SENSOR_INFO_QUEUE, dlProps).build();
    }

    @Bean
    public Queue ruleEngineSensorInfoQueue(RabbitDeadLetterProperties dlProps) {
        return DeadLetterQueues.durableWithDeadLetter(RULE_ENGINE_SENSOR_INFO_QUEUE, dlProps).build();
    }

    @Bean
    public Binding dataSourceSensorInfoBinding(@Qualifier("dataSourceSensorInfoQueue") Queue dataSourceSensorInfoQueue) {
        return BindingBuilder
                .bind(dataSourceSensorInfoQueue)
                .to(sensorExchange())
                .with(SENSOR_INFO_BINDING_KEY_PATTERN);
    }

    @Bean
    public Binding ruleEngineSensorInfoBinding(@Qualifier("ruleEngineSensorInfoQueue") Queue ruleEngineSensorInfoQueue) {
        return BindingBuilder
                .bind(ruleEngineSensorInfoQueue)
                .to(sensorExchange())
                .with(SENSOR_INFO_BINDING_KEY_PATTERN);
    }

    @Bean
    public Queue ruleEngineThresholdInfoQueue(RabbitDeadLetterProperties dlProps) {
        return DeadLetterQueues.durableWithDeadLetter(RULE_ENGINE_THRESHOLD_INFO_QUEUE, dlProps).build();
    }

    @Bean
    public Binding ruleEngineThresholdInfoBinding(@Qualifier("ruleEngineThresholdInfoQueue") Queue ruleEngineThresholdInfoQueue) {
        return BindingBuilder
                .bind(ruleEngineThresholdInfoQueue)
                .to(sensorExchange())
                .with(THRESHOLD_INFO_BINDING_KEY_PATTERN);
    }

    @Bean
    public Queue dataSourceThresholdInfoQueue(RabbitDeadLetterProperties dlProps) {
        return DeadLetterQueues.durableWithDeadLetter(DATA_SOURCE_THRESHOLD_INFO_QUEUE, dlProps).build();
    }

    @Bean
    public Binding dataSourceThresholdInfoBinding(@Qualifier("dataSourceThresholdInfoQueue") Queue dataSourceThresholdInfoQueue) {
        return BindingBuilder
                .bind(dataSourceThresholdInfoQueue)
                .to(sensorExchange())
                .with(THRESHOLD_INFO_BINDING_KEY_PATTERN);
    }

    @Bean
    public Queue environmentComplianceRequestQueue(RabbitDeadLetterProperties dlProps) {
        return DeadLetterQueues.durableWithDeadLetter(ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE, dlProps).build();
    }

    @Bean
    public Binding environmentComplianceRequestBinding(@Qualifier("environmentComplianceRequestQueue") Queue environmentComplianceRequestQueue) {
        return BindingBuilder
                .bind(environmentComplianceRequestQueue)
                .to(sensorExchange())
                .with(ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE);
    }

    // AI 관련
    @Bean
    public DirectExchange harvestExchange() {
        return new DirectExchange(HARVEST_EXCHANGE);
    }

    @Bean
    public Queue aiHarvestQueue(RabbitDeadLetterProperties dlProps) {
        return DeadLetterQueues.durableWithDeadLetter(AI_HARVEST_QUEUE, dlProps).build();
    }

    @Bean
    public Binding aiHarvestBinding(@Qualifier("aiHarvestQueue") Queue aiHarvestQueue) {
        return BindingBuilder
                .bind(aiHarvestQueue)
                .to(harvestExchange())
                .with(AI_HARVEST_QUEUE);
    }

    // 알림 관련
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }
}
