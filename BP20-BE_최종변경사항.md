# BP20-BE 변경사항 상세 정리 (최신 main pull 기준, 최종본)

`https://github.com/AIVLE-07-20-big-project/BP20-BE`에서 새로 받은 최신 `main` 대비, 이번에 커밋한 모든 변경사항입니다.

---

## 한눈에 요약

| 구분 | 개수 | 내용 |
|---|---|---|
| 수정된 파일 | 7개 | 아래 표 참고 |
| 삭제된 파일 | 0개 | (`Dockerfile` 삭제를 검토했으나 최종적으로 보류 — 이유는 맨 아래 참고) |
| 신규 파일 | 0개 | (CI 워크플로우 추가를 검토했으나 최종적으로 보류 — 이유는 맨 아래 참고) |

---

## 수정된 파일 7개

### 1. `src/main/resources/application.yml`

**문제**: 예전에 GitHub 웹 에디터로 PR 충돌을 해결하던 중, 브랜치 이름 텍스트가 실수로 파일 안에 같이 들어간 적이 있었습니다. 나중에 팀원이 발견해서 `#`으로 주석 처리해 급한 문법 오류는 막아뒀지만, 의미 없는 텍스트 자체는 파일에 남아있었습니다.

**변경**: 찌꺼기 주석 2줄 완전 삭제
```diff
- # feat/receipt-ocr-integration-v2
  ocr-service:
    base-url: ${OCR_SERVICE_BASE_URL:http://localhost:8000}
  ...
- # main
```

---

### 2. `src/main/java/com/bp20/backend/api/receipt/client/OcrServiceClient.java`

**문제**: PR 리뷰 대응 중 로컬에서 고쳤던 수정사항이 실제로는 push되지 않은 채 PR이 merge되어, `main`에는 예전(버그 있는) 버전이 남아있었습니다. `@RequiredArgsConstructor`로 `RestClient` 빈을 직접 주입받으려 했는데 그 빈을 제공하던 옛날 설정 클래스는 이미 삭제된 상태라, 기동 시 `No qualifying bean of type 'RestClient'` 에러 발생.

**변경 1 — 생성자 재작성**: 팀 공용 `RestClient.Builder`(`ExternalRestClientConfig` 제공)에 이 서비스만의 `base-url`(`OcrServiceProperties`)을 얹어 직접 조립하도록 수정.
```diff
- import lombok.RequiredArgsConstructor;
+ import com.bp20.backend.global.config.OcrServiceProperties;

  @Slf4j
  @Component
- @RequiredArgsConstructor
  public class OcrServiceClient {
      private final RestClient ocrServiceRestClient;

+     public OcrServiceClient(RestClient.Builder externalRestClientBuilder, OcrServiceProperties properties) {
+         this.ocrServiceRestClient = externalRestClientBuilder.clone()
+                 .baseUrl(properties.baseUrl())
+                 .build();
+     }
```

**변경 2 — Swagger 자물쇠 표시 추가**: 이 컨트롤러들은 인증 기능이 생기기 전에 만들어서 `@SecurityRequirement` 표시가 없었습니다. 그 결과 Swagger UI가 "이 엔드포인트는 인증 불필요"로 착각해 Authorize에 입력한 토큰을 요청에 자동으로 안 실어 보내, 실제로는 인증이 필요한데도 계속 401만 떴습니다. (`ReceiptController`, `ReceiptAnalyticsController`, `BudgetController` 공통 — 4·5·6번에도 동일 적용)

---

### 3. `src/main/java/com/bp20/backend/global/config/ExternalRestClientConfig.java`

**문제**: `OcrServiceProperties`(`ocr-service.base-url` 값을 담는 설정 클래스)를 Spring Bean으로 등록해주는 역할이 예전엔 이미 삭제된 `OcrServiceClientConfig`에 있었는데, 그 등록 역할이 어디로도 안 옮겨진 채 `main`에 남아있었습니다. `No qualifying bean of type 'OcrServiceProperties'` 에러로 기동 실패.

**변경**:
```diff
+ import org.springframework.boot.context.properties.EnableConfigurationProperties;

  @Configuration
+ @EnableConfigurationProperties(OcrServiceProperties.class)
  public class ExternalRestClientConfig {
```

---

### 4. `src/main/java/com/bp20/backend/api/receipt/controller/ReceiptController.java`

**문제 1**: URL 경로가 `/api/receipts`로, 팀의 `SecurityConfig` 권한 규칙(`/api/store-owner/**` → `STORE_OWNER_ACCESS` 권한 필요)에 맞지 않는 경로라 `anyRequest().authenticated()`에 걸려 인증만으론 부족한 401 발생.

**문제 2**: `@SecurityRequirement(name = "bearerAuth")` 누락으로 Swagger에 자물쇠 아이콘 미표시 (2번 항목과 동일 원인).

**변경**:
```diff
+ import io.swagger.v3.oas.annotations.security.SecurityRequirement;

  @RestController
- @RequestMapping("/api/receipts")
+ @RequestMapping("/api/store-owner/receipts")
  @RequiredArgsConstructor
+ @SecurityRequirement(name = "bearerAuth")
  public class ReceiptController {
```

---

### 5. `src/main/java/com/bp20/backend/api/receipt/controller/ReceiptAnalyticsController.java`

4번과 동일한 이유로 동일하게 수정:
```diff
- @RequestMapping("/api/analytics")
+ @RequestMapping("/api/store-owner/analytics")
  @RequiredArgsConstructor
+ @SecurityRequirement(name = "bearerAuth")
  public class ReceiptAnalyticsController {
```

---

### 6. `src/main/java/com/bp20/backend/api/budget/controller/BudgetController.java`

4번과 동일한 이유로 동일하게 수정:
```diff
- @RequestMapping("/api/budgets")
+ @RequestMapping("/api/store-owner/budgets")
  @RequiredArgsConstructor
+ @SecurityRequirement(name = "bearerAuth")
  public class BudgetController {
```

---

### 7. `src/main/java/com/bp20/backend/api/receipt/dto/request/ReceiptCreateRequest.java`

**문제**: 실제 저장 테스트 중 발견 — `force` 필드가 원시타입 `boolean`으로 되어있어서, 요청 JSON에 이 필드를 빠뜨리면 `Cannot map null into type boolean`이라는 500 에러가 났습니다.

**변경**: `boolean` → `Boolean`(래퍼 타입)으로 바꾸고, 컴팩트 생성자로 `null`이면 자동으로 `false` 처리:
```diff
- boolean force
+ Boolean force
  ) {
+     public ReceiptCreateRequest {
+         if (force == null) {
+             force = false;
+         }
+     }
  }
```
`ReceiptService.java`의 `request.force()` 사용 부분은 `Boolean`이 자동으로 `boolean`으로 언박싱되어 **코드 수정 없이 그대로 호환**됩니다.

---

## `compose.yaml` — 파일 수정 아니지만 함께 반영

**변경 이유**: 팀 결정 — Spring Boot는 더 이상 Docker 컨테이너로 실행하지 않기로 함 (MySQL만 Docker로 계속 띄우고, Spring Boot는 `gradlew bootRun`으로 직접 실행).

**변경 내용**: `mysql` 서비스는 유지, `springboot` 서비스 블록만 제거.
```diff
  services:
    mysql:
      ...

- # Spring Boot
- springboot:
-   build: .
-   container_name: spring_boot_bp20
-   ...
```

---

## 검토했지만 최종적으로 손대지 않은 것 (중요)

### `Dockerfile` (레포 루트) — 삭제하지 않음

처음엔 "Spring Boot를 Docker로 안 쓰니 필요 없다"고 판단해서 삭제를 검토했지만, **확인해보니 팀원이 이미 만들어둔 CI 파이프라인(`chore/ci-pipeline`, PR #7)의 `docker-build` job이 이 파일을 그대로 참조하고 있었습니다**:
```yaml
docker-build:
  ...
  - name: Build Docker image
    uses: docker/build-push-action@v7
    with:
      file: ./Dockerfile
```
"로컬 개발에서 Docker로 Spring Boot를 안 띄운다"는 것과 "CI에서 배포용 이미지를 빌드해본다"는 것은 별개일 수 있어 판단을 보류했습니다. **`Dockerfile`은 원본 그대로 유지**했습니다.

### `.github/workflows/backend-ci.yml` — 추가하지 않음

이번 사건(로컬에서만 고치고 push 안 한 코드가 그대로 merge됨) 재발 방지용으로 간단한 CI를 만들어뒀었는데, **이미 팀원이 훨씬 더 완성도 높은 CI**(테스트 포함 빌드 + 의존성 취약점 검사 + Docker 이미지 빌드까지)를 만들어서 `main`에 merge해뒀다는 걸 확인했습니다. 중복 추가하지 않았습니다.

---

## 코드는 아니지만 실행 환경에 필요한 것 (참고용, 커밋 대상 아님)

`.env`(gitignore 대상이라 커밋 안 됨)에 아래 값이 있어야 정상 동작합니다:
```
OCR_SERVICE_BASE_URL=http://localhost:8001
```
(OCR 서비스가 별도 레포(`BP20-AI`)에서 `services/receipt-ocr-analytics/`로 재구조화되면서 기본 포트가 8000 → 8001로 바뀐 것을 반영. `application.yml`의 기본값은 아직 8000으로 되어있어서, 이 값을 `.env`에 명시하지 않으면 8000으로 시도해 연결 실패함.)

---

## 검증 완료된 것

이번 수정사항들을 전부 반영한 상태로, 아래 흐름을 **실제 데이터로 끝까지 확인**했습니다:

```
슈퍼관리자 로그인 → 점주 초대 → 회원가입 → 점주 로그인
  → 영수증 이미지 업로드 → OCR 파싱 성공 (실제 상호명/품목/금액 인식)
  → 영수증 DB 저장 성공 (중복탐지 정상 작동 확인)
  → 이상지출/예산초과 분석 API 정상 응답
  → 통합 HTML 리포트 정상 생성 (매입단가 추이 그래프 등 실데이터 반영)
```

**"총매출 0원"은 알려진 제약**입니다 — `Order`/`Product` 엔티티가 아직 이 백엔드에 없어서 발생하며, 버그가 아닙니다 (다른 팀원의 작업 대기 중).