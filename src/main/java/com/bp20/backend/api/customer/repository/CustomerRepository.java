package com.bp20.backend.api.customer.repository;

import com.bp20.backend.api.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
