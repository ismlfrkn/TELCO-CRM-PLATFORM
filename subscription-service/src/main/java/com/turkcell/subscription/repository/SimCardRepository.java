package com.turkcell.subscription.repository;

import com.turkcell.subscription.entity.SimCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimCardRepository extends JpaRepository<SimCard, String> {

    boolean existsByImsi(String imsi);
}
