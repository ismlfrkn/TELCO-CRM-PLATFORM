package com.turkcell.subscription.repository;

import com.turkcell.subscription.entity.MsisdnPool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MsisdnPoolRepository extends JpaRepository<MsisdnPool, String> {

    Page<MsisdnPool> findAllByStatus(String status, Pageable pageable);

    /**
     * FOR UPDATE SKIP LOCKED: birden fazla istek ayni anda musait numara isterse, biri satiri
     * kilitler, digerleri o satiri beklemek yerine bir sonraki musait satiri alir. Bu sayede
     * iki abonelige ayni MSISDN'in verilmesi engellenir ve istekler birbirini bloklamaz.
     */
    @Query(value = "SELECT * FROM msisdn_pool WHERE status = 'FREE' ORDER BY msisdn ASC LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<MsisdnPool> lockNextFree();
}
