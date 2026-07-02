package com.turkcell.productcatalog.repository;

import com.turkcell.productcatalog.entity.ProductOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductOfferingRepository extends JpaRepository<ProductOffering, UUID> {
    
    Optional<ProductOffering> findByCode(String code);

    Optional<ProductOffering> findByCodeAndStatusNot(String code, String status);

    Page<ProductOffering> findAllByStatusNot(String status, Pageable pageable);
}
