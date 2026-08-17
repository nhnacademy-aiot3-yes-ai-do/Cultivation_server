# RabbitMQ Smoke Test

## 용도

`RabbitSmokeController`, `RabbitSmokeService`, 요청 DTO는 Cultivation Server에서
Rule Engine 및 Data Source로 전달되는 RabbitMQ 이벤트를 HTTP 요청으로 확인하기 위한
임시 테스트 기능입니다.

- DB에 센서를 등록하지 않고 RabbitMQ 이벤트만 발행합니다.
- 운영용 API가 아닙니다.
- 관련 컴포넌트는 `rabbit-smoke` 프로필에서만 등록됩니다.

## 실행 전 준비

프로젝트 루트의 `.env`에 애플리케이션 실행에 필요한 DB, RabbitMQ, MinIO, InfluxDB
환경 변수가 설정되어 있어야 합니다.

RabbitMQ 연결에 직접 사용하는 값은 다음과 같습니다.

```properties
RABBITMQ_HOST=...
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=...
RABBITMQ_PASSWORD=...
```

## 실행

프로젝트 루트에서 `rabbit-smoke` 프로필을 활성화하여 실행합니다.

```bash
SPRING_PROFILES_ACTIVE=rabbit-smoke ./mvnw spring-boot:run
```

Maven 옵션으로 활성화할 수도 있습니다.

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=rabbit-smoke
```

빌드한 JAR를 실행할 때는 다음과 같이 지정합니다.

```bash
java -jar target/cultivation_server-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=rabbit-smoke \
  --server.port=8080
```

별도의 `server.port` 설정이 없으므로 기본 대상 포트는 `8080`입니다. 다른 포트를
사용한다면 실행 시 `--server.port=<포트>`를 지정하고 `http/req.http`의
`rabbitSmoke` 변수도 같은 포트로 변경합니다.

## 호출

엔드포인트는 다음과 같습니다.

```text
POST http://localhost:8080/internal/test/rabbit/sensor-created
```

IntelliJ HTTP Client에서는 프로젝트 루트의 `http/req.http`에서 RabbitMQ smoke 요청을
실행합니다. 정상적으로 요청이 접수되면 `202 Accepted`를 반환합니다. 이는 이벤트 발행
요청을 수락했다는 의미이며, 최종 소비까지 성공했다는 의미는 아닙니다. Producer 로그,
RabbitMQ 관리 화면 또는 소비자 로그로 실제 전달 결과를 추가 확인합니다.

## 404 확인

다음과 같이 프로필을 활성화하지 않고 실행하면 `RabbitSmokeController`가 등록되지
않으므로 해당 경로의 `404 Not Found`는 정상 동작입니다.

```bash
./mvnw spring-boot:run
```

404가 발생하면 다음 순서로 확인합니다.

1. 실행 명령에 `rabbit-smoke` 프로필이 포함되었는지 확인합니다.
2. 애플리케이션이 실제로 사용하는 포트와 요청 포트가 같은지 확인합니다.
3. 프로필을 변경한 뒤 애플리케이션을 재시작했는지 확인합니다.
4. 요청 경로가 `/internal/test/rabbit/sensor-created`인지 확인합니다.

실행 포트는 다음 명령으로 확인할 수 있습니다.

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```
