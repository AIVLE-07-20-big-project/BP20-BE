package com.bp20.backend.global.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 목록 조회 API에서 공통으로 쓰는 페이지네이션 응답. Spring Data의 Page를 그대로 노출하지 않고
 * 프론트가 필요로 하는 필드만 뽑아서 응답한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
