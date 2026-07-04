package com.turkcell.payment.repository;

import com.turkcell.payment.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * MSISDN havuzundaki "herhangi bir musait satiri al" ihtiyacinin aksine, burada belirli/tek bir
     * cuzdanin bakiyesi degistirilecek - SKIP LOCKED uygun degil, transaction'in kilidi BEKLEMESI
     * gerekir. PESSIMISTIC_WRITE, ayni cuzdana eszamanli iki odemenin ikisinin de "yeterli bakiye var"
     * sanip bakiyeyi negatife dusurmesini engeller.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);
}
