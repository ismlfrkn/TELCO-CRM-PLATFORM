package com.turkcell.usage.repository;

import com.turkcell.usage.entity.CdrEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CdrEventRepository extends JpaRepository<CdrEvent, UUID> {

    Optional<CdrEvent> findByExternalCdrId(String externalCdrId);
}
