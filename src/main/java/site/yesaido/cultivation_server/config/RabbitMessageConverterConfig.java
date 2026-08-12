package site.yesaido.cultivation_server.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.rabbitmq.event.*;

import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class RabbitMessageConverterConfig {

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("threshold.crud", ThresholdInfoEvent.class);
        idClassMapping.put("sensor.upsert", SensorInfoUpsertEvent.class);
        idClassMapping.put("sensor.delete", SensorInfoDeleteEvent.class);
        idClassMapping.put("sensorValueEvent", SensorValueEvent.class);
        idClassMapping.put("environmentComplianceRequest", EnvironmentComplianceRequest.class);
        idClassMapping.put("environmentComplianceResponse", EnvironmentComplianceResponse.class);
        classMapper.setIdClassMapping(idClassMapping);

        return classMapper;
    }

    @Bean
    public MessageConverter jsonMessageConverter(DefaultClassMapper classMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

        converter.setClassMapper(classMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);     // JSON 컨버터 등록

        // 필요 시 추가 설정
        // ...

        return rabbitTemplate;
    }
}
