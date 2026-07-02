package com.turkcell.productcatalog.repository;

import com.turkcell.productcatalog.entity.Tariff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, UUID> {
    
    Optional<Tariff> findByCode(String code);

    Optional<Tariff> findByCodeAndStatusNot(String code, String status);

    Page<Tariff> findAllByStatusNot(String status, Pageable pageable);
}
