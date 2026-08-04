package com.bp20.backend.api.receipt.client;

import com.bp20.backend.api.budget.domain.Budget;
import com.bp20.backend.api.order.domain.MenuItem;
import com.bp20.backend.api.order.domain.Order;
import com.bp20.backend.api.receipt.domain.Receipt;
import com.bp20.backend.api.receipt.domain.ReceiptItem;
import com.bp20.backend.api.receipt.dto.response.BudgetOverageResponse;
import com.bp20.backend.api.receipt.dto.response.ExpenseAnomalyResponse;
import com.bp20.backend.api.receipt.dto.response.OcrParseResponse;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.config.OcrServiceProperties;
import com.bp20.backend.global.response.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 영수증 OCR + 가계부/원가분석 Python 마이크로서비스 호출 클라이언트.
 * 이 서비스는 상태를 갖지 않으므로(stateless), 매 호출마다 필요한 데이터를 전부 함께 보낸다.
 *
 * HTTP 클라이언트 자체(타임아웃, HttpClient5 등)는 팀 공용 설정인
 * {@link com.bp20.backend.global.config.ExternalRestClientConfig}의 빌더를 그대로 재사용하고,
 * 이 클래스에서는 OCR 서비스의 base-url만 지정해서 clone해 쓴다.
 */
@Slf4j
@Component
public class OcrServiceClient {

    private final RestClient ocrServiceRestClient;
    private final ObjectMapper objectMapper;

    public OcrServiceClient(
            RestClient.Builder externalRestClientBuilder,
            OcrServiceProperties properties,
            ObjectMapper objectMapper
    ) {
        this.ocrServiceRestClient = externalRestClientBuilder.clone()
                .baseUrl(properties.baseUrl())
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 영수증 이미지를 Python 서비스로 보내 OCR 결과를 받는다. (DB 저장은 하지 않음 - 미리보기 용도)
     */
    public OcrParseResponse parseReceipt(
            MultipartFile file,
            List<ProductCatalogPayload> productCatalog
    ) {
        try {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt.jpg";
                }
            };

            // 파일 파트에 Content-Type을 명시적으로 지정해야 FastAPI가 정상적인
            // UploadFile 파트로 인식한다 (안 붙이면 파트 자체를 인식 못 해 422가 날 수 있음).
            HttpHeaders fileHeaders = new HttpHeaders();
            MediaType fileContentType = file.getContentType() != null
                    ? MediaType.parseMediaType(file.getContentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
            fileHeaders.setContentType(fileContentType);
            HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(fileResource, fileHeaders);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", filePart);
            body.add("productCatalog", objectMapper.writeValueAsString(productCatalog));

            return ocrServiceRestClient.post()
                    .uri("/api/v1/receipts/parse")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OcrParseResponse.class);

        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_INPUT, e);
        } catch (RestClientResponseException e) {
            // Python 서비스가 4xx/5xx를 응답한 경우 - 실제 실패 사유(response body)를 로그로 남긴다.
            log.error("[OcrServiceClient] 영수증 파싱 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.OCR_SERVICE_UNAVAILABLE, e);
        } catch (RestClientException e) {
            // 연결 자체가 안 된 경우 (Python 서비스 다운 등)
            log.error("[OcrServiceClient] 영수증 파싱 요청 실패 (연결 불가)", e);
            throw new ApiException(ErrorCode.OCR_SERVICE_UNAVAILABLE, e);
        }
    }

    public record ProductCatalogPayload(String name, long price) {
    }


    /**
     * 카테고리별 주간 지출 이상 탐지 (2번 AI 가계부).
     * receipts는 통계 계산의 모수가 되므로, 보통 매장의 전체 기간 영수증을 넘긴다.
     */
    public List<ExpenseAnomalyResponse> getExpenseAnomalies(List<Receipt> receipts, double zThreshold) {
        List<ReceiptPayload> payload = receipts.stream().map(ReceiptPayload::from).toList();
        ExpenseAnomalyRequest request = new ExpenseAnomalyRequest(payload, zThreshold);

        try {
            List<ExpenseAnomalyResponse> result = ocrServiceRestClient.post()
                    .uri("/api/v1/analytics/expense-anomalies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ExpenseAnomalyResponse>>() {});
            return result != null ? result : List.of();
        } catch (RestClientResponseException e) {
            log.error("[OcrServiceClient] 이상 지출 탐지 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.OCR_SERVICE_UNAVAILABLE, e);
        } catch (RestClientException e) {
            log.error("[OcrServiceClient] 이상 지출 탐지 요청 실패 (연결 불가)", e);
            throw new ApiException(ErrorCode.OCR_SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 월별·카테고리별 예산 초과 확인 (2번 AI 가계부).
     */
    public List<BudgetOverageResponse> getBudgetOverage(List<Receipt> receipts, List<Budget> budgets) {
        List<ReceiptPayload> receiptPayload = receipts.stream().map(ReceiptPayload::from).toList();
        List<BudgetPayload> budgetPayload = budgets.stream().map(BudgetPayload::from).toList();
        BudgetOverageRequest request = new BudgetOverageRequest(receiptPayload, budgetPayload);

        try {
            List<BudgetOverageResponse> result = ocrServiceRestClient.post()
                    .uri("/api/v1/analytics/budget-overage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BudgetOverageResponse>>() {});
            return result != null ? result : List.of();
        } catch (RestClientResponseException e) {
            log.error("[OcrServiceClient] 예산 초과 확인 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.OCR_SERVICE_UNAVAILABLE, e);
        } catch (RestClientException e) {
            log.error("[OcrServiceClient] 예산 초과 확인 요청 실패 (연결 불가)", e);
            throw new ApiException(ErrorCode.OCR_SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 통합 HTML 리포트(경영 장부) 생성. 월간/연간/총기간 중 선택.
     *
     * menuItems/orders는 각각 MenuItemRepository/OrderRepository에서 매장 기준으로 조회한 값을
     * 그대로 받아 Python 쪽이 기대하는 camelCase 필드(productId, orderedDate 등)로 변환해 보낸다.
     * 두 값이 비어 있으면(아직 CSV를 업로드하지 않은 매장) Python 쪽에서 매출 0원/빈 원가율 테이블로
     * 안전하게 처리한다 - 그 방어 로직은 그대로 유지된다.
     */
    public String getReport(List<Receipt> receipts, List<Budget> budgets, List<ReceiptItem> items,
                             List<MenuItem> menuItems, List<Order> orders,
                             String storeName, String reportType, Integer year, Integer month) {
        List<ReceiptPayload> receiptPayload = receipts.stream().map(ReceiptPayload::from).toList();
        List<BudgetPayload> budgetPayload = budgets.stream().map(BudgetPayload::from).toList();
        List<ReceiptItemPayload> itemPayload = items.stream().map(ReceiptItemPayload::from).toList();
        List<ProductPayload> productPayload = menuItems.stream().map(ProductPayload::from).toList();
        List<OrderPayload> orderPayload = orders.stream().map(OrderPayload::from).toList();

        ReportRequest request = new ReportRequest(
                receiptPayload, budgetPayload, itemPayload,
                productPayload, orderPayload,
                storeName, reportType, year, month
        );

        try {
            String html = ocrServiceRestClient.post()
                    .uri("/api/v1/analytics/report")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            return html != null ? html : "";
        } catch (RestClientResponseException e) {
            log.error("[OcrServiceClient] 리포트 생성 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.OCR_SERVICE_UNAVAILABLE, e);
        } catch (RestClientException e) {
            log.error("[OcrServiceClient] 리포트 생성 요청 실패 (연결 불가)", e);
            throw new ApiException(ErrorCode.OCR_SERVICE_UNAVAILABLE, e);
        }
    }

    private record ExpenseAnomalyRequest(List<ReceiptPayload> receipts, double zThreshold) {
    }

    private record BudgetOverageRequest(List<ReceiptPayload> receipts, List<BudgetPayload> budgets) {
    }

    private record ReportRequest(List<ReceiptPayload> receipts, List<BudgetPayload> budgets,
                                  List<ReceiptItemPayload> items, List<ProductPayload> products, List<OrderPayload> orders,
                                  String storeName, String reportType, Integer year, Integer month) {
    }
}
