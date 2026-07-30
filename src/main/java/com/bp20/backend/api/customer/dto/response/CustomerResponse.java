package com.bp20.backend.api.customer.dto.response;

import com.bp20.backend.api.customer.domain.Customer;
import com.bp20.backend.api.customer.domain.CustomerStatus;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        CustomerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getEmail(),
                customer.getName(),
                customer.getPhoneNumber(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
