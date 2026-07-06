package com.turkcell.notification.repository;

import com.turkcell.notification.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    Optional<Channel> findByCode(String code);

    boolean existsByCode(String code);
}
