package com.turkcell.usage.repository;

import com.turkcell.usage.entity.Quota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotaRepository extends JpaRepository<Quota, UUID> {

    Optional<Quota> findBySubscriptionId(UUID subscriptionId);

    /**
     * Ayni aboneligin kotasina eszamanli birden fazla CDR dusme istegi gelirse (ornegin ayni anda
     * biten iki cagri), PESSIMISTIC_WRITE bu satiri kilitleyip digerlerini BEKLETIR - Wallet'taki
     * bakiye dususuyle ayni gerekce (MsisdnPool'daki SKIP LOCKED'in aksine burada "bekle" gerekir,
     * cunku hepsi ayni satiri guncellemek zorunda, "baskasini al" secenegi yok).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Quota q WHERE q.subscriptionId = :subscriptionId")
    Optional<Quota> findBySubscriptionIdForUpdate(@Param("subscriptionId") UUID subscriptionId);
}
