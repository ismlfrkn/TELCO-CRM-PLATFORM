package com.turkcell.productcatalog.repository;

import com.turkcell.productcatalog.entity.TariffVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TariffVersionRepository extends JpaRepository<TariffVersion, UUID> {

    Optional<TariffVersion> findByCodeAndVersion(String code, int version);

    List<TariffVersion> findAllByCodeOrderByVersionDesc(String code);
}
