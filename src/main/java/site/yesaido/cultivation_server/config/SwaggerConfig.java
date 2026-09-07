package site.yesaido.cultivation_server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cultivation Server의 OpenAPI(Swagger) 문서 메타데이터를 정의합니다.
 * <p>
 * 엔드포인트별 설명은 {@code controller/docs} 패키지의 {@code *ControllerDocs} 인터페이스에 두고,
 * 문서 노출 범위(경로 매칭/제외)와 Swagger UI 경로는 {@code application.yml}의 {@code springdoc.*}에서 설정합니다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cultivationOpenAPI(@Value("${server.port:9001}") int serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("Cultivation Server API")
                        .version("v1")
                        .description("""
                                버섯 재배 관리 서비스 API.

                                재배(Cultivation) · 멤버 · 사진 · 수확 · 센서 · 환경 설정/준수율 · 버섯 레퍼런스 · 센서 타입을 제공합니다.

                                ### 인증
                                모든 요청은 API Gateway를 통해 들어오며, Gateway가 JWT를 검증한 뒤
                                `X-User-Id`(필요 시 `X-User-Role`) 헤더를 주입합니다.
                                서비스를 직접 호출할 때는 해당 헤더를 직접 넣어야 합니다.

                                ### 오류 응답
                                오류는 RFC 7807 `application/problem+json` 형식으로 반환되며
                                `status`, `title`, `detail`, `code`, `instance` 필드를 포함합니다.

                                ### 문서 범위
                                `/api/v1/internal/**` 내부 연동 API와 테스트용 엔드포인트는 문서에서 제외됩니다.
                                """))
                .servers(List.of(
                        new Server().url("https://api.yes-nhn.site").description("운영 Gateway"),
                        new Server().url("http://localhost:8000").description("로컬 Gateway"),
                        new Server().url("http://localhost:" + serverPort).description("로컬 직접 호출")
                ));
    }
}
