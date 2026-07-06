package com.turkcell.notification.repository;

import com.turkcell.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByCodeAndChannelIdAndLocale(String code, UUID channelId, String locale);

    boolean existsByCodeAndChannelIdAndLocale(String code, UUID channelId, String locale);
}
