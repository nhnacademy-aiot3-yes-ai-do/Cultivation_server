package site.yesaido.cultivation_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class CultivationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CultivationServerApplication.class, args);
    }

}
