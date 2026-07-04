package com.turkcell.notification.service;

import com.turkcell.notification.dto.request.NotificationTemplateCreateRequest;
import com.turkcell.notification.dto.response.NotificationTemplateResponse;
import com.turkcell.notification.entity.Channel;
import com.turkcell.notification.entity.NotificationTemplate;
import com.turkcell.notification.exception.DuplicateNotificationTemplateException;
import com.turkcell.notification.exception.NotificationTemplateNotFoundException;
import com.turkcell.notification.mapper.NotificationTemplateMapper;
import com.turkcell.notification.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final ChannelService channelService;
    private final NotificationTemplateMapper notificationTemplateMapper;

    public NotificationTemplateService(NotificationTemplateRepository notificationTemplateRepository,
                                        ChannelService channelService,
                                        NotificationTemplateMapper notificationTemplateMapper) {
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.channelService = channelService;
        this.notificationTemplateMapper = notificationTemplateMapper;
    }

    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateCreateRequest request) {
        Channel channel = channelService.getChannelByCode(request.getChannelCode());

        if (notificationTemplateRepository.existsByCodeAndChannelIdAndLocale(
                request.getCode(), channel.getId(), request.getLocale())) {
            throw new DuplicateNotificationTemplateException(
                    "Template already exists for code=" + request.getCode()
                            + ", channel=" + request.getChannelCode() + ", locale=" + request.getLocale());
        }

        NotificationTemplate template = new NotificationTemplate();
        template.setCode(request.getCode());
        template.setChannelId(channel.getId());
        template.setLocale(request.getLocale());
        template.setSubject(request.getSubject());
        template.setBodyTemplate(request.getBodyTemplate());

        return notificationTemplateMapper.toResponse(notificationTemplateRepository.save(template));
    }

    public NotificationTemplate getTemplate(String code, UUID channelId, String locale) {
        return notificationTemplateRepository.findByCodeAndChannelIdAndLocale(code, channelId, locale)
                .orElseThrow(() -> new NotificationTemplateNotFoundException(
                        "Template not found for code=" + code + ", channelId=" + channelId + ", locale=" + locale));
    }
}
