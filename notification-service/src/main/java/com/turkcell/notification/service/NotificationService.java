package com.turkcell.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.notification.dto.request.NotificationSendRequest;
import com.turkcell.notification.dto.response.NotificationResponse;
import com.turkcell.notification.entity.Channel;
import com.turkcell.notification.entity.Notification;
import com.turkcell.notification.entity.NotificationTemplate;
import com.turkcell.notification.mapper.NotificationMapper;
import com.turkcell.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * FR-28/FR-29: gercek bir SMS/e-posta/push saglayicisi entegre edilmedi (dokuman bolum 6.2 - "mock
 * kanal" MVP kapsaminda). Sablon + kanal dogrulandiktan sonra "gonderim" sadece loglanir ve
 * Notification kaydi SENT olarak isaretlenir - mock PSP'nin (payment-service) ayni sadelik seviyesi.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String AGGREGATE_TYPE = "Notification";

    private final NotificationRepository notificationRepository;
    private final ChannelService channelService;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationMapper notificationMapper;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository, ChannelService channelService,
                                NotificationTemplateService notificationTemplateService,
                                NotificationMapper notificationMapper, OutboxEventService outboxEventService,
                                ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.channelService = channelService;
        this.notificationTemplateService = notificationTemplateService;
        this.notificationMapper = notificationMapper;
        this.outboxEventService = outboxEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public NotificationResponse send(NotificationSendRequest request) {
        Channel channel = channelService.getChannelByCode(request.getChannelCode());
        NotificationTemplate template = notificationTemplateService.getTemplate(
                request.getTemplateCode(), channel.getId(), request.getLocale());

        String renderedBody = render(template.getBodyTemplate(), request.getPayload());

        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTemplateCode(request.getTemplateCode());
        notification.setChannelId(channel.getId());
        notification.setPayloadJson(toJson(request.getPayload()));
        notification.setStatus(Notification.STATUS_SENT);
        notification.setSentAt(Instant.now());
        notification = notificationRepository.save(notification);

        log.info("Mock {} notification dispatched to user {} (template={}): subject=[{}] body=[{}]",
                channel.getCode(), request.getUserId(), request.getTemplateCode(), template.getSubject(), renderedBody);

        NotificationResponse response = notificationMapper.toResponse(notification);
        outboxEventService.publish(AGGREGATE_TYPE, notification.getId(), "NotificationDispatched", response);
        return response;
    }

    public Page<NotificationResponse> getHistoryForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByUserIdOrderBySentAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    private String render(String template, Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
