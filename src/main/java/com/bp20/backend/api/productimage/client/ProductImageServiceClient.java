package com.bp20.backend.api.productimage.client;

import com.bp20.backend.api.productimage.dto.response.CategoriesResponse;
import com.bp20.backend.global.config.ProductImageServiceProperties;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
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

/**
 * AI 상품 이미지 생성 Python 마이크로서비스 호출 클라이언트.
 * 이 서비스도 상태를 갖지 않으므로(stateless), 매 호출마다 파일 + 메뉴명을 함께 보낸다.
 *
 * HTTP 클라이언트 자체(타임아웃, HttpClient5 등)는 팀 공용 설정인
 * {@link com.bp20.backend.global.config.ExternalRestClientConfig}의 빌더를 그대로 재사용하고,
 * 이 클래스에서는 상품 이미지 서비스의 base-url만 지정해서 clone해 쓴다.
 *
 * ⚠️ 이 서비스는 호출 1건당 실제 비용(OpenAI API 과금)이 발생하므로,
 * 프론트/서비스 계층에서 중복 호출 방지 로직을 별도로 고려할 것.
 */
@Slf4j
@Component
public class ProductImageServiceClient {

    private final RestClient productImageRestClient;

    public ProductImageServiceClient(
            RestClient.Builder externalRestClientBuilder,
            ProductImageServiceProperties properties
    ) {
        this.productImageRestClient = externalRestClientBuilder.clone()
                .baseUrl(properties.baseUrl())
                .build();
    }

    /**
     * 지원하는 메뉴(카테고리) 목록을 조회한다.
     */
    public CategoriesResponse getCategories() {
        try {
            CategoriesResponse result = productImageRestClient.get()
                    .uri("/api/v1/product-images/categories")
                    .retrieve()
                    .body(CategoriesResponse.class);
            return result != null ? result : new CategoriesResponse(java.util.List.of());
        } catch (RestClientResponseException e) {
            log.error("[ProductImageServiceClient] 카테고리 조회 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.PRODUCT_IMAGE_SERVICE_UNAVAILABLE, e);
        } catch (RestClientException e) {
            log.error("[ProductImageServiceClient] 카테고리 조회 요청 실패 (연결 불가)", e);
            throw new ApiException(ErrorCode.PRODUCT_IMAGE_SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 상품 사진 + 메뉴명을 Python 서비스로 보내, 배경이 합성된 이미지를 바이트 배열로 받는다.
     * OpenAI API 응답 대기 시간(수 초~수십 초)이 포함되므로 타임아웃에 유의할 것.
     */
    public byte[] generateProductImage(MultipartFile file, String category) {
        try {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "product.jpg";
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
            body.add("category", category);

            return productImageRestClient.post()
                    .uri("/api/v1/product-images/generate")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);

        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_INPUT, e);
        } catch (RestClientResponseException e) {
            // Python 서비스가 4xx(잘못된 카테고리 등)/5xx(OpenAI 오류 등)를 응답한 경우
            log.error("[ProductImageServiceClient] 상품 이미지 생성 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.PRODUCT_IMAGE_SERVICE_UNAVAILABLE, e);
        } catch (RestClientException e) {
            // 연결 자체가 안 된 경우 (Python 서비스 다운 등)
            log.error("[ProductImageServiceClient] 상품 이미지 생성 요청 실패 (연결 불가)", e);
            throw new ApiException(ErrorCode.PRODUCT_IMAGE_SERVICE_UNAVAILABLE, e);
        }
    }
}
