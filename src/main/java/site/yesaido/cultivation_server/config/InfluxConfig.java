package site.yesaido.cultivation_server.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InfluxProperties.class)
public class InfluxConfig {

    @Bean(destroyMethod = "close")
    public InfluxDBClient influxDBClient(InfluxProperties properties) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        if (properties.getCloudflare().isConfigured()) {
            httpClient.addInterceptor(chain -> chain.proceed(
                    chain.request().newBuilder()
                            .addHeader("CF-Access-Client-Id", properties.getCloudflare().getAccessClientId())
                            .addHeader("CF-Access-Client-Secret", properties.getCloudflare().getAccessClientSecret())
                            .build()
            ));
        }

        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(required(properties.getUrl(), "influx.url"))
                .authenticateToken(required(properties.getToken(), "influx.token").toCharArray())
                .org(required(properties.getOrg(), "influx.org"))
                .bucket(required(properties.getBucket(), "influx.bucket"))
                .okHttpClient(httpClient)
                .build();

        return InfluxDBClientFactory.create(options);
    }

    private String required(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " must be configured");
        }
        return value;
    }
}