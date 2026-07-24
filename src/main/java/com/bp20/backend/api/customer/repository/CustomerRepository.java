package com.bp20.backend.api.customer.repository;

import com.bp20.backend.api.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByStoreIdAndPrivateInfoEmailIgnoreCase(Long storeId, String email);

    @Query("select c from Customer c "
            + "join fetch c.privateInfo "
            + "where c.store.id = :storeId order by c.id desc")
    List<Customer> findAllInStore(@Param("storeId") Long storeId);

    @Query("select c from Customer c "
            + "join fetch c.privateInfo "
            + "where c.id = :customerId and c.store.owner.id = :ownerId")
    Optional<Customer> findOwnedCustomer(
            @Param("customerId") Long customerId,
            @Param("ownerId") Long ownerId
    );
}
