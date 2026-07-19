package com.turkcell.productcatalog.repository;

import com.turkcell.productcatalog.entity.Addon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddonRepository extends JpaRepository<Addon, UUID> {

    Optional<Addon> findByCode(String code);

    Optional<Addon> findByCodeAndStatusNot(String code, String status);

    Page<Addon> findAllByStatusNot(String status, Pageable pageable);

    @Query("SELECT a FROM Addon a JOIN a.tariffs t WHERE t.code = :tariffCode AND a.status != 'INACTIVE'")
    Page<Addon> findAllByTariffCodeAndActive(@Param("tariffCode") String tariffCode, Pageable pageable);
}
