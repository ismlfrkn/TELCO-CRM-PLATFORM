package com.turkcell.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.notification.dto.request.NotificationSendRequest;
import com.turkcell.notification.dto.response.NotificationResponse;
import com.turkcell.notification.entity.Channel;
import com.turkcell.notification.entity.Notification;
import com.turkcell.notification.entity.NotificationTemplate;
import com.turkcell.notification.exception.ChannelNotFoundException;
import com.turkcell.notification.exception.NotificationTemplateNotFoundException;
import com.turkcell.notification.mapper.NotificationMapper;
import com.turkcell.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private ChannelService channelService;
    private NotificationTemplateService notificationTemplateService;
    private OutboxEventService outboxEventService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        channelService = mock(ChannelService.class);
        notificationTemplateService = mock(NotificationTemplateService.class);
        outboxEventService = mock(OutboxEventService.class);
        NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);

        notificationService = new NotificationService(notificationRepository, channelService,
                notificationTemplateService, notificationMapper, outboxEventService, new ObjectMapper());

        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            if (notification.getId() == null) {
                notification.setId(UUID.randomUUID());
            }
            return notification;
        });
    }

    @Test
    void send_withValidTemplate_savesAsSentAndPublishesNotificationDispatched() {
        Channel channel = channelWith("SMS");
        NotificationTemplate template = templateWith(channel.getId(), "Merhaba {{firstName}}, hattiniz aktif.");
        when(channelService.getChannelByCode("SMS")).thenReturn(channel);
        when(notificationTemplateService.getTemplate("WELCOME_SMS", channel.getId(), "tr-TR")).thenReturn(template);

        NotificationSendRequest request = new NotificationSendRequest();
        request.setUserId(UUID.randomUUID());
        request.setTemplateCode("WELCOME_SMS");
        request.setChannelCode("SMS");
        request.setLocale("tr-TR");
        request.setPayload(Map.of("firstName", "Serhat"));

        NotificationResponse response = notificationService.send(request);

        assertThat(response.getStatus()).isEqualTo(Notification.STATUS_SENT);
        assertThat(response.getSentAt()).isNotNull();
        verify(outboxEventService).publish(eq("Notification"), any(), eq("NotificationDispatched"), any());
    }

    @Test
    void send_whenChannelMissing_throwsChannelNotFoundException() {
        when(channelService.getChannelByCode("FAX")).thenThrow(new ChannelNotFoundException("Channel not found with code: FAX"));

        NotificationSendRequest request = new NotificationSendRequest();
        request.setUserId(UUID.randomUUID());
        request.setTemplateCode("WELCOME_SMS");
        request.setChannelCode("FAX");

        assertThatThrownBy(() -> notificationService.send(request)).isInstanceOf(ChannelNotFoundException.class);
        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void send_whenTemplateMissing_throwsNotificationTemplateNotFoundException() {
        Channel channel = channelWith("SMS");
        when(channelService.getChannelByCode("SMS")).thenReturn(channel);
        when(notificationTemplateService.getTemplate("GHOST", channel.getId(), "tr-TR"))
                .thenThrow(new NotificationTemplateNotFoundException("Template not found"));

        NotificationSendRequest request = new NotificationSendRequest();
        request.setUserId(UUID.randomUUID());
        request.setTemplateCode("GHOST");
        request.setChannelCode("SMS");

        assertThatThrownBy(() -> notificationService.send(request))
                .isInstanceOf(NotificationTemplateNotFoundException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getHistoryForUser_returnsMappedPage() {
        UUID userId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setStatus(Notification.STATUS_SENT);
        when(notificationRepository.findAllByUserIdOrderBySentAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationResponse> page = notificationService.getHistoryForUser(userId, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo(userId);
    }

    private Channel channelWith(String code) {
        Channel channel = new Channel();
        channel.setId(UUID.randomUUID());
        channel.setCode(code);
        return channel;
    }

    private NotificationTemplate templateWith(UUID channelId, String bodyTemplate) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(UUID.randomUUID());
        template.setChannelId(channelId);
        template.setBodyTemplate(bodyTemplate);
        return template;
    }
}
