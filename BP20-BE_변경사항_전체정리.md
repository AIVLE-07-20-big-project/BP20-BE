# BP20-BE 변경사항 전체 정리

GitHub에서 최초로 clone한 시점(main 브랜치) 대비, 영수증 OCR/AI 가계부 기능을 연동하며 추가·수정된 모든 내용을 정리합니다.

---

## 1. 한눈에 보는 요약

| 구분 | 내용 |
|---|---|
| 새로 추가된 도메인 | `Receipt`(영수증), `ReceiptItem`(영수증 품목), `Budget`(예산) |
| 새로 추가된 폴더 | `ocr_service/`(Python FastAPI 서비스 전체) |
| 새로 추가된 API 엔드포인트 | 8개 (아래 3번 참고) |
| 새로 생성된 DB 테이블 | `receipts`, `receipt_items`, `budgets` |
| 수정된 기존 파일 | `application.yml`, `ErrorCode.java`, `SuccessCode.java`, `build.gradle` |
| 추가 Gradle 의존성 | `org.apache.httpcomponents.client5:httpclient5` 1개 |
| 데이터 일괄 등록 도구 | `import_csv_to_backend.py` (CSV → API 자동 등록) |

---

## 2. 새로 추가된 파일 (Java)

### 2-1. `api/receipt` 패키지 (영수증 도메인) — 전부 신규

```
src/main/java/com/bp20/backend/api/receipt/
├── domain/
│   ├── Receipt.java              엔티티. StoreID는 Store 엔티티가 아직 없어 Long 컬럼으로 임시 처리
│   ├── ReceiptItem.java          엔티티. Receipt와 다대일 관계
│   └── ReceiptStatus.java        CONFIRMED / NEEDS_REVIEW / DUPLICATE_SUSPECTED
├── repository/
│   └── ReceiptRepository.java    findByDedupeKey, findByStoreId 등
├── dto/request/
│   └── ReceiptCreateRequest.java 저장 확정 요청 (OCR 결과를 사용자가 검토 후 보내는 값)
├── dto/response/
│   ├── OcrParseResponse.java     Python /parse 응답 전체 매핑
│   ├── ReceiptParseResult.java   Python /parse의 "result" 객체 매핑
│   ├── ReceiptItemData.java      품목 1건 매핑 (요청·응답 공용)
│   ├── ReceiptResponse.java      저장된 영수증 조회 응답
│   ├── ExpenseAnomalyResponse.java   이상 지출 탐지 결과 매핑
│   └── BudgetOverageResponse.java    예산 초과 확인 결과 매핑
├── client/
│   ├── OcrServiceClient.java     Python 서비스 호출 클라이언트 (핵심 파일)
│   ├── ReceiptPayload.java       Java Receipt → Python으로 보낼 camelCase JSON 변환
│   ├── ReceiptItemPayload.java   Java ReceiptItem → Python 전송용 변환
│   └── BudgetPayload.java        Java Budget → Python 전송용 변환
├── service/
│   ├── ReceiptService.java       OCR 파싱 위임 + 저장(중복탐지 포함) + 조회
│   └── ReceiptAnalyticsService.java  DB 조회 후 Python에 분석/리포트 생성 위임
└── controller/
    ├── ReceiptController.java            영수증 파싱/저장/조회 API
    └── ReceiptAnalyticsController.java   이상지출/예산초과/리포트 API
```

**`OcrServiceClient.java`에 있는 메서드 4개:**
- `parseReceipt(MultipartFile)` — 영수증 이미지 OCR 요청
- `getExpenseAnomalies(List<Receipt>, double)` — 이상 지출 탐지 요청
- `getBudgetOverage(List<Receipt>, List<Budget>)` — 예산 초과 확인 요청
- `getReport(List<Receipt>, List<Budget>, List<ReceiptItem>, String, String, Integer, Integer)` — HTML 통합 리포트 생성 요청 *(나중에 추가됨, 5번 참고)*

### 2-2. `api/budget` 패키지 (예산 도메인) — 전부 신규

```
src/main/java/com/bp20/backend/api/budget/
├── domain/
│   └── Budget.java                storeId, yearMonth(컬럼명은 budget_month), category, budgetAmount
├── repository/
│   └── BudgetRepository.java
├── dto/request/
│   └── BudgetCreateRequest.java   예산 등록 요청 (storeId, yearMonth, category, budgetAmount)
├── dto/response/
│   └── BudgetResponse.java
├── service/
│   └── BudgetService.java         같은 (매장,월,카테고리) 조합이면 upsert(덮어쓰기)
└── controller/
    └── BudgetController.java      POST /api/budgets
```

### 2-3. `global/config` 패키지에 파일 2개 추가 (기존 폴더, 파일만 신규)

```
src/main/java/com/bp20/backend/global/config/
├── OcrServiceProperties.java      application.yml의 ocr-service.base-url 바인딩
└── OcrServiceClientConfig.java    Python 호출용 RestClient Bean 정의
```

**`OcrServiceClientConfig.java` 상세 내용:**
- Spring Boot 기본 HTTP 클라이언트(JDK 내장 `java.net.http.HttpClient`)가 **파일 업로드(멀티파트) 요청에서 불안정**한 문제가 실제로 발생하여(요청은 갔는데 파일 필드를 못 알아보거나, 연결이 중간에 끊기는 문제), **Apache HttpClient5로 명시적으로 교체**
- 연결 타임아웃 10초, 응답 타임아웃 180초(3분)로 설정 — PaddleOCR 처리가 CPU 환경에서 느릴 수 있어 기본값보다 넉넉하게 설정

---

## 3. 새로 추가된 API 엔드포인트 (총 8개)

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/api/receipts/parse` | 영수증 이미지 업로드 → OCR 결과 미리보기 (DB 저장 안 함) |
| POST | `/api/receipts` | 검토·확정된 영수증 저장 (중복 의심 시 409) |
| GET | `/api/receipts/{receiptId}` | 영수증 단건 조회 |
| GET | `/api/receipts?storeId=` | 매장별 영수증 목록 조회 |
| GET | `/api/analytics/expense-anomalies?storeId=&zThreshold=` | 이상 지출 탐지 |
| GET | `/api/analytics/budget-overage?storeId=` | 예산 초과 확인 |
| POST | `/api/budgets` | 예산 등록/수정 (upsert) |
| GET | `/api/analytics/report?storeId=&storeName=&reportType=&year=&month=` | **HTML 통합 리포트 생성** (월간/연간/총기간) — JSON이 아니라 HTML을 그대로 반환 |

---

## 4. 기존 파일 수정 내역

### 4-1. `src/main/resources/application.yml`

맨 아래에 추가:
```yaml
ocr-service:
  base-url: ${OCR_SERVICE_BASE_URL:http://localhost:8000}
```

### 4-2. `src/main/java/com/bp20/backend/global/response/ErrorCode.java`

3개 항목 추가:
```java
NOT_FOUND_RECEIPT(HttpStatus.NOT_FOUND, "영수증을 찾을 수 없습니다."),
CONFLICT_DUPLICATE_RECEIPT(HttpStatus.CONFLICT, "동일한 거래로 보이는 영수증이 이미 등록되어 있습니다."),
OCR_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "OCR/분석 서비스와 통신할 수 없습니다."),
```

### 4-3. `src/main/java/com/bp20/backend/global/response/SuccessCode.java`

6개 항목 추가:
```java
SUCCESS_RECEIPT_PARSE(HttpStatus.OK, "영수증 OCR 인식을 완료했습니다."),
SUCCESS_RECEIPT_CREATE(HttpStatus.CREATED, "영수증을 저장했습니다."),
SUCCESS_RECEIPT_GET(HttpStatus.OK, "영수증 정보를 조회했습니다."),
SUCCESS_ANALYTICS_EXPENSE_ANOMALIES(HttpStatus.OK, "이상 지출 탐지 결과를 조회했습니다."),
SUCCESS_ANALYTICS_BUDGET_OVERAGE(HttpStatus.OK, "예산 초과 확인 결과를 조회했습니다."),
SUCCESS_BUDGET_CREATE(HttpStatus.CREATED, "예산을 등록했습니다."),
```
(리포트 엔드포인트는 JSON이 아니라 HTML을 직접 반환하므로 `ApiResponse`로 감싸지 않아, SuccessCode 항목이 별도로 필요 없음)

### 4-4. `build.gradle`

`dependencies { }` 블록에 1줄 추가:
```gradle
implementation 'org.apache.httpcomponents.client5:httpclient5'
```
(JDK 기본 HttpClient의 멀티파트 불안정 문제 회피용. 이 의존성이 없으면 `OcrServiceClientConfig.java`가 컴파일되지 않음)

---

## 5. 리포트(HTML) 기능 추가 상세

`GET /api/analytics/report`를 추가하면서 아래 파일들이 추가로 수정되었습니다.

### 5-1. Java

- **`OcrServiceClient.java`**: `getReport(...)` 메서드 및 내부 `ReportRequest` record 추가. Python에 `products`, `orders`는 아직 Java에 해당 엔티티가 없어 항상 빈 리스트(`List.of()`)로 전송.
- **`ReceiptAnalyticsService.java`**: `getReport(storeId, storeName, reportType, year, month)` 추가. 매장의 모든 영수증에서 품목(`ReceiptItem`)까지 펼쳐서 Python에 전달.
- **`ReceiptAnalyticsController.java`**: `GET /api/analytics/report` 추가. `produces = MediaType.TEXT_HTML_VALUE`로 지정해 브라우저에서 바로 열람 가능한 HTML을 반환.

### 5-2. Python (`ocr_service/`)

- **`main.py`**:
  - `_orders_to_df()`: 매출(orders) 데이터가 비어있을 때도 `OrderedDate` 컬럼이 datetime 타입을 유지하도록 수정 (안 그러면 이후 날짜 연산에서 타입 에러 발생)
  - `analytics_report()` 엔드포인트에 요청 수신 로그 추가 (`receipts=N건, budgets=N건...` 형태로 콘솔에 출력 — 문제 진단용)
- **`build_report.py`**:
  - `build_html_from_frames()`의 리포트 기간(날짜 범위) 계산 로직 보강:
    - 매출 데이터가 없어도(현재 Java 쪽 상황) 지출 데이터만으로 기간 계산이 되도록 처리
    - `dropna()`로 빈 값(NaT)을 걸러낸 뒤 최소/최대 날짜를 계산 — 안 그러면 `'float' object has no attribute 'date'` 라는 알기 어려운 에러로 500이 발생했음 (실제로 겪은 문제, 아래 8번 표 참고)
    - 지출 데이터 자체가 하나도 없는 극단적인 경우, 알 수 없는 에러 대신 **"리포트를 생성할 지출(영수증) 데이터가 없습니다. storeId가 맞는지 확인해주세요"** 라는 명확한 메시지로 400 에러를 반환하도록 개선

### 5-3. 현재 한계 (알고 있는 제약)

**"총매출"이 항상 0원으로 나옵니다.** 이는 버그가 아니라, Java 백엔드에 아직 `Order`(매출/주문) 엔티티가 없어서 리포트 요청 시 매출 데이터를 아예 보내지 못하기 때문입니다. 마찬가지로 "메뉴별 원가율" 섹션도 `Product`(메뉴/판매가) 엔티티가 없어 항상 빈 테이블로 나옵니다.

→ 나중에 팀원이 `Store`, `Product`, `Order` 엔티티를 추가하면, `OcrServiceClient.getReport()` 호출 시 넘기는 `products`/`orders` 인자를 실제 데이터로 채우기만 하면 매출·원가율까지 완전한 리포트가 나옵니다. (Python 쪽은 이미 매출 데이터를 받아 처리할 준비가 되어 있음 — 실제로 빈 데이터로 검증까지 마침)

---

## 6. 데이터베이스 변경 (JPA `ddl-auto: update`로 자동 생성됨)

로컬 MySQL에 새 테이블 3개가 생성됩니다 (기존 팀 스키마에 있던 테이블은 건드리지 않음):

- **`receipts`**: receipt_id, store_id, uploaded_by_user_id, document_type, vendor_name, business_number, transaction_date, transaction_time, payment_method, category, supply_amount, vat, tax_free_amount, total_amount, status, dedupe_key(unique), raw_image_path, created_at, updated_at
- **`receipt_items`**: receipt_item_id, receipt_id(FK), line_number, item_name, quantity, unit, unit_price, total_price, matched_product_id
- **`budgets`**: budget_id, store_id, budget_month, category, budget_amount, created_at, updated_at

> ⚠️ `budgets` 테이블의 실제 DB 컬럼명은 `budget_month`입니다 (Java 필드명은 `yearMonth` 그대로). MySQL에서 `year_month`를 컬럼명으로 쓰면 `INTERVAL` 구문과 충돌해서 문법 오류가 나는 걸 실제로 겪고 나서 바꾼 것입니다.

### 실제로 채워 넣은 데이터 (온기카페 테스트 데이터)

`import_csv_to_backend.py`로 2년치(2024-07~2026-06) 가상 카페 데이터를 일괄 등록해서 검증했습니다:
- 영수증 351건 (+품목 500여 건)
- 예산 144건

---

## 7. 새로 추가된 폴더/파일 (Java가 아닌 것들)

### 7-1. `ocr_service/` 폴더 (완전히 새로운 Python 마이크로서비스)

```
ocr_service/
├── main.py                FastAPI 앱. 6개 엔드포인트(OCR 파싱 + 분석 4종 + 리포트) 노출
├── receipt_pipeline.py    OCR 전처리, PaddleOCR 호출, 상호명/품목 추출, 검증
├── csv_store.py           (로컬 CLI 실행용, 마이크로서비스 자체에는 미사용)
├── expense_analysis.py    이상지출/예산초과 계산 로직
├── cost_analysis.py       매입단가추적/원가율 계산 로직
├── build_report.py        HTML 통합 리포트 생성 (월간/연간/총기간)
├── requirements.txt       Python 의존성 목록
└── Dockerfile             컨테이너 빌드용 (compose.yaml과 함께 사용 가능)
```

이 폴더는 Spring Boot 애플리케이션과 별도의 독립 프로세스로 실행됩니다 (`uvicorn main:app`). Java 코드 관점에서는 `OcrServiceClient`가 HTTP로 호출하는 외부 서버일 뿐입니다.

### 7-2. 루트에 추가된 스크립트/문서

```
BP20-BE/
├── import_csv_to_backend.py   기존 CSV(영수증/예산) 데이터를 API로 일괄 등록하는 스크립트
├── INTEGRATION_GUIDE.md       Java-Python 연동 아키텍처/설치 가이드
└── cafe_synthetic_data/       (사용자가 직접 배치) 온기카페 2년치 테스트용 CSV 6개
```

---

## 8. 개발 중 실제로 겪은 버그와 수정 이력 (참고용)

이 항목들은 "왜 지금 이 코드가 이런 모습인지"를 설명하기 위한 기록입니다.

| 문제 | 원인 | 조치 |
|---|---|---|
| `budgets` 테이블 생성 시 SQL 문법 오류 | 컬럼명 `year_month`가 MySQL `INTERVAL` 키워드와 충돌 | 컬럼명을 `budget_month`로 변경 (Java 필드명은 유지) |
| `POST /api/receipts/parse` 호출 시 502 | Python 서비스(`ocr_service`)가 안 떠있거나 재시작됨 | 3개 프로세스(MySQL/Python/Spring) 모두 켜져 있는지 확인하는 절차 정립 |
| 같은 요청이 422 (`file` 필드 누락) | JDK 기본 HttpClient의 멀티파트 인코딩 문제로 파일 파트가 제대로 전달 안 됨 | `OcrServiceClientConfig`를 Apache HttpClient5로 교체 |
| 요청이 3분 뒤 타임아웃 (`Read timed out`) | PaddleOCR 엔진을 **매 요청마다 새로 생성**하고 있어서 처리 시간이 과도하게 걸림 | `receipt_pipeline.py`에서 OCR 엔진을 모듈 레벨에 캐싱(1회만 로드) + FastAPI 시작 시점에 미리 예열(`@app.on_event("startup")`) |
| `GET /api/analytics/report` 호출 시 500 (`AttributeError: 'float' object has no attribute 'date'`) | 매출(orders) 데이터가 없는 상태에서 날짜 최소/최대 계산 시 빈 값(NaN)이 섞여 `.date()` 호출이 깨짐 | `build_report.py`에 `dropna()` 방어 코드 추가 + 지출 데이터 자체가 없는 경우 명확한 400 에러 메시지로 개선 |

---

## 9. 아직 안 된 것 (다음 단계로 남겨둔 부분)

- **`Store`, `Product`, `Order` 엔티티가 이 백엔드에 아직 없음** — `Receipt.storeId`는 관계(FK)가 아니라 순수 `Long` 컬럼으로 임시 처리되어 있습니다. `Store` 엔티티가 팀원에 의해 추가되면 `@ManyToOne Store store`로 교체하는 게 맞습니다.
- **리포트의 "총매출", "메뉴별 원가율"이 항상 비어있음** — `Product`/`Order` 엔티티가 없어서 (5-3번 참고). Python 쪽은 이미 이 데이터를 받아 처리할 준비가 되어 있으므로, Java 쪽에 엔티티만 추가되면 바로 연결 가능.
- **인증/권한 미적용** — 지금 추가한 모든 API는 로그인 여부와 무관하게 열려 있습니다. `feat/identity-access-management` 브랜치의 인증 기능과 통합이 필요합니다.
