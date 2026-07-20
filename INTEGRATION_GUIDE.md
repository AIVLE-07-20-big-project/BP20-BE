# BP20-BE 연동 가이드 — 영수증 OCR / AI 가계부

## 아키텍처

```
┌─────────────────────┐        HTTP (JSON/multipart)        ┌──────────────────────────┐
│  Spring Boot (Java)  │ ───────────────────────────────────▶│  FastAPI (Python)        │
│  BP20-BE             │                                      │  ocr_service              │
│                       │                                      │                          │
│  - MySQL에 Receipt/   │◀───────────────────────────────────  │  - PaddleOCR             │
│    ReceiptItem/Budget │        구조화된 JSON 결과 반환         │  - pandas 기반 계산      │
│    저장 (실제 DB)      │                                      │  - 상태 없음(stateless) │
└─────────────────────┘                                      └──────────────────────────┘
```

- **Java 백엔드가 진짜 데이터 저장소**를 가지고 있고 (MySQL), 실제 서비스 로직/인증/권한은 전부 Java 쪽에서 담당합니다.
- **Python 서비스는 "계산 전용" 서버**입니다. 상태(데이터)를 갖지 않고, 매 요청마다 필요한 데이터를 Java가 함께 보내주면 계산 결과만 돌려줍니다.
- 이렇게 나눈 이유: PaddleOCR·pandas 기반 계산 로직은 Java로 옮기기 어렵고(성숙한 대안이 마땅치 않음), 반대로 인증·DB·트랜잭션 관리는 Java/Spring이 훨씬 낫기 때문입니다.

## 폴더 구성

```
BP20-BE/                         (기존 Java 레포)
├── ocr_service/                 (신규 - Python FastAPI 서비스, 이번에 추가)
│   ├── main.py
│   ├── receipt_pipeline.py
│   ├── csv_store.py
│   ├── expense_analysis.py
│   ├── cost_analysis.py
│   ├── build_report.py
│   ├── requirements.txt
│   └── Dockerfile
└── src/main/java/com/bp20/backend/
    ├── api/receipt/             (신규 - 영수증 도메인)
    │   ├── domain/              Receipt.java, ReceiptItem.java, ReceiptStatus.java
    │   ├── repository/          ReceiptRepository.java
    │   ├── dto/request/         ReceiptCreateRequest.java
    │   ├── dto/response/        OcrParseResponse.java, ReceiptParseResult.java,
    │   │                        ReceiptItemData.java, ReceiptResponse.java,
    │   │                        ExpenseAnomalyResponse.java, BudgetOverageResponse.java
    │   ├── client/               OcrServiceClient.java, ReceiptPayload.java,
    │   │                        ReceiptItemPayload.java, BudgetPayload.java
    │   ├── service/              ReceiptService.java, ReceiptAnalyticsService.java
    │   └── controller/           ReceiptController.java, ReceiptAnalyticsController.java
    ├── api/budget/               (신규 - 예산 도메인, 최소 구현)
    │   ├── domain/               Budget.java
    │   └── repository/           BudgetRepository.java
    └── global/config/            (기존 폴더에 파일만 추가)
        ├── OcrServiceProperties.java
        └── OcrServiceClientConfig.java
```

## 설치/실행 순서

### 1. Python 서비스 실행

```bash
cd ocr_service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

`http://localhost:8000/health` 로 `{"status":"ok"}` 나오면 정상입니다.
`http://localhost:8000/docs` 에서 Swagger UI로 모든 엔드포인트를 바로 테스트해볼 수 있습니다 (FastAPI 자동 생성).

### 2. Java 쪽 파일 배치

위 폴더 구성대로 파일들을 `src/main/java/com/bp20/backend/` 밑에 그대로 복사해 넣으세요.
**추가 Gradle 의존성은 필요 없습니다** — `RestClient`는 `spring-boot-starter-web`에 이미 포함되어 있습니다.

### 3. `application.yml` 수정

`config-snippets/application-yml-addition.yml` 내용을 기존 `application.yml` 맨 아래에 추가하세요.

### 4. `ErrorCode.java` / `SuccessCode.java` 수정

`config-snippets/ErrorCode-SuccessCode-additions.txt`에 있는 항목들을 각 enum에 추가하세요.

### 5. (선택) Docker Compose로 같이 띄우기

`config-snippets/compose-yaml-addition.yml`을 참고해서 기존 `compose.yaml`에 `ocr_service`를 추가하고,
`springboot` 서비스의 `environment`/`depends_on`도 함께 수정하세요.

```bash
docker compose up --build
```

## API 사용 흐름

### 1) 영수증 업로드 → 미리보기

```
POST /api/receipts/parse  (multipart/form-data, file=이미지)
```
→ OCR 결과를 반환 (DB 저장 안 함). 프론트에서 이 결과를 사용자에게 보여주고 수정하게 함.

### 2) 검토 완료 → 저장 확정

```
POST /api/receipts
Content-Type: application/json

{
  "storeId": 1,
  "documentType": "RECEIPT",
  "storeName": "가온원두상회",
  "transactionDate": "2026-06-01",
  "transactionTime": "09:15",
  "paymentMethod": "카드",
  "items": [{"itemName": "원두(생두)", "quantity": 10, "unit": "kg", "unitPrice": 24000, "totalPrice": 240000}],
  "totalAmount": 240000,
  "category": "식재료비",
  "force": false
}
```
- 중복 의심이면 `409 CONFLICT_DUPLICATE_RECEIPT` 응답 → 사용자에게 확인 후 `force: true`로 재요청

### 3) 가계부 분석 조회

```
GET /api/analytics/expense-anomalies?storeId=1&zThreshold=1.3
GET /api/analytics/budget-overage?storeId=1
```

## 아직 안 된 것 (다음 단계)

- **`cost-rates`(원가율), `report`(HTML 통합 리포트) 엔드포인트는 Java 쪽에 연결 안 함** — `Product`, `Order`, `Store` 엔티티가 이 백엔드에 아직 없어서, 팀원들이 그 도메인을 먼저 만들어야 자연스럽게 이어붙일 수 있습니다. Python 서비스 쪽엔 `/api/v1/analytics/cost-rates`, `/api/v1/analytics/report` 엔드포인트가 이미 준비돼 있으니, 저 엔티티들이 생기면 `OcrServiceClient`에 메서드만 추가하면 됩니다.
- **`Store` 엔티티가 없어서 `storeId`를 그냥 `Long` 컬럼으로 임시 처리**했습니다. 나중에 `Store` 엔티티가 생기면 `@ManyToOne Store store`로 바꾸는 게 맞습니다.
- Java 코드는 이 샌드박스에 Maven 저장소 접근이 안 되어 **컴파일 검증을 못 했습니다**. `./gradlew build`로 로컬에서 확인해주세요. Python 쪽은 실제로 서버 띄워서 4개 분석 엔드포인트 + OCR 파싱 엔드포인트 + HTML 리포트 엔드포인트 전부 테스트 완료했습니다.
