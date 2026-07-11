package com.turkcell.customer.repository;

import com.turkcell.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndDeletedFalse(UUID id);

    Page<Customer> findAllByDeletedFalse(Pageable pageable);

    boolean existsByIdentityNumber(String identityNumber);
}
