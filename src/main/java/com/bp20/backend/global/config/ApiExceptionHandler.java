package com.bp20.backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 잘못된 CSV 형식, 숫자 변환 실패, 필수 헤더 누락 등
     * 사용자가 수정할 수 있는 입력 오류를 처리한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    /**
     * 상품·매출·재고 CSV가 아직 업로드되지 않은 상태에서
     * 추천 생성을 요청하는 경우 등을 처리한다.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleIllegalState(
            IllegalStateException exception
    ) {
        return createErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    /**
     * @Valid 검증 실패를 처리한다.
     *
     * 현재 DTO에 @NotNull, @Min 등의 검증을 추가했을 때 사용된다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .collect(Collectors.joining(", "));

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    /**
     * multipart/form-data 요청에서 file 파트가 누락된 경우 처리한다.
     *
     * 예:
     * Body의 key가 file이 아니거나
     * 파일을 선택하지 않은 경우
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMissingRequestPart(
            MissingServletRequestPartException exception
    ) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "필수 요청 데이터가 없습니다: "
                        + exception.getRequestPartName()
        );
    }

    /**
     * 필수 request parameter가 누락된 경우 처리한다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMissingRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "필수 요청 파라미터가 없습니다: "
                        + exception.getParameterName()
        );
    }

    /**
     * application.yml에 설정한 업로드 최대 용량을 초과한 경우 처리한다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String, Object> handleMaxUploadSize(
            MaxUploadSizeExceededException exception
    ) {
        return createErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "업로드 가능한 파일 크기를 초과했습니다."
        );
    }

    /**
     * 위에서 별도로 처리하지 못한 예외를 처리한다.
     *
     * 실제 운영 환경에서는 예외 내용을 그대로 사용자에게
     * 노출하지 않고 서버 로그에만 기록하는 것이 좋다.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(
            Exception exception
    ) {
        exception.printStackTrace();

        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 처리 중 오류가 발생했습니다."
        );
    }

    /**
     * 공통 오류 응답 형식을 생성한다.
     */
    private Map<String, Object> createErrorResponse(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);

        return response;
    }
}