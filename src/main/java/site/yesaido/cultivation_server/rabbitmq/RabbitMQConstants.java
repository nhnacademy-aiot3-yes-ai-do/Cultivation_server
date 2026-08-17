package site.yesaido.cultivation_server.rabbitmq;

public final class RabbitMQConstants {

    private RabbitMQConstants() {}

    // Dead Letter 관련
    public static final String DLX_NAME = "yes-nhn.dlx";
    public static final String DLX_KEY = "x-dead-letter-exchange";
    public static final String DLQ_QUEUE = "yes-nhn.dlq";

    // 센서 관련
    public static final String SENSOR_EXCHANGE = "yes-nhn.sensor.exchange";
    public static final String DATA_SOURCE_SENSOR_INFO_QUEUE = "yes-nhn.data-source.sensor-info.queue";
    public static final String RULE_ENGINE_SENSOR_INFO_QUEUE = "yes-nhn.rule-engine.sensor-info.queue";
    public static final String SENSOR_INFO_BINDING_KEY_PATTERN = "yes-nhn.#.sensor-info.queue";
    public static final String RULE_ENGINE_THRESHOLD_INFO_QUEUE = "yes-nhn.rule-engine.threshold-info.queue";
    public static final String DATA_SOURCE_THRESHOLD_INFO_QUEUE = "yes-nhn.data-source.threshold-info.queue";
    public static final String THRESHOLD_INFO_BINDING_KEY_PATTERN = "yes-nhn.#.threshold-info.queue";
//    public static final String SENSOR_SENSOR_VALUE_QUEUE = "yes-nhn.sensor.sensor-value.queue";
    public static final String ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE = "yes-nhn.environment.compliance.queue";

    // AI 관련
    public static final String HARVEST_EXCHANGE = "yes-nhn.harvest.exchange";
    public static final String AI_HARVEST_QUEUE = "yes-nhn.ai.harvest.queue";

    // 알림 관련
    public static final String NOTIFICATION_EXCHANGE = "yes-nhn.notification.exchange";
    public static final String NOTIFICATION_DONE_QUEUE = "yes-nhn.notification.done.queue";
}