package site.yesaido.cultivation_server.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.yesaido.common.storage.DefaultStorageUrlResolver;
import site.yesaido.common.storage.StorageUrlResolver;

@Configuration
public class MinioConfig {
    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public StorageUrlResolver storageUrlResolver() {
        // ReDoS 검토: "(^/+)|(/+$)"는 중첩 수량자나 겹치는 반복 구조가 없는 단순 flat quantifier라
        // catastrophic backtracking이 발생하지 않고 입력 길이에 선형으로 처리됨.
        // 게다가 minioUrl은 사용자 입력이 아니라 서버 설정값(@Value)이라 공격 벡터 자체가 없음.
        String baseUrl = minioUrl.replaceAll("(^/+)|(/+$)", "");
        return new DefaultStorageUrlResolver(baseUrl, bucket, baseUrl);
    }
}
