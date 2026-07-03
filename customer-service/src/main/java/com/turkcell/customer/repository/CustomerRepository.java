package com.turkcell.customer.repository;

import com.turkcell.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndDeletedFalse(UUID id);

    boolean existsByIdentityNumber(String identityNumber);
}
