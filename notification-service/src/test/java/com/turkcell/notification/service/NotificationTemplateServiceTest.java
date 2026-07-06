package com.turkcell.notification.service;

import com.turkcell.notification.dto.request.NotificationTemplateCreateRequest;
import com.turkcell.notification.dto.response.NotificationTemplateResponse;
import com.turkcell.notification.entity.Channel;
import com.turkcell.notification.entity.NotificationTemplate;
import com.turkcell.notification.exception.DuplicateNotificationTemplateException;
import com.turkcell.notification.exception.NotificationTemplateNotFoundException;
import com.turkcell.notification.mapper.NotificationTemplateMapper;
import com.turkcell.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationTemplateServiceTest {

    private NotificationTemplateRepository notificationTemplateRepository;
    private ChannelService channelService;
    private NotificationTemplateService notificationTemplateService;

    @BeforeEach
    void setUp() {
        notificationTemplateRepository = mock(NotificationTemplateRepository.class);
        channelService = mock(ChannelService.class);
        NotificationTemplateMapper mapper = Mappers.getMapper(NotificationTemplateMapper.class);
        notificationTemplateService = new NotificationTemplateService(notificationTemplateRepository, channelService, mapper);

        when(notificationTemplateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createTemplate_withNewCombination_succeeds() {
        Channel channel = channelWith("SMS");
        when(channelService.getChannelByCode("SMS")).thenReturn(channel);
        when(notificationTemplateRepository.existsByCodeAndChannelIdAndLocale("WELCOME_SMS", channel.getId(), "tr-TR"))
                .thenReturn(false);

        NotificationTemplateCreateRequest request = new NotificationTemplateCreateRequest();
        request.setCode("WELCOME_SMS");
        request.setChannelCode("SMS");
        request.setLocale("tr-TR");
        request.setBodyTemplate("Merhaba {{firstName}}, hattiniz aktif edildi.");

        NotificationTemplateResponse response = notificationTemplateService.createTemplate(request);

        assertThat(response.getCode()).isEqualTo("WELCOME_SMS");
        assertThat(response.getChannelId()).isEqualTo(channel.getId());
    }

    @Test
    void createTemplate_withExistingCombination_throwsDuplicateNotificationTemplateException() {
        Channel channel = channelWith("SMS");
        when(channelService.getChannelByCode("SMS")).thenReturn(channel);
        when(notificationTemplateRepository.existsByCodeAndChannelIdAndLocale("WELCOME_SMS", channel.getId(), "tr-TR"))
                .thenReturn(true);

        NotificationTemplateCreateRequest request = new NotificationTemplateCreateRequest();
        request.setCode("WELCOME_SMS");
        request.setChannelCode("SMS");
        request.setLocale("tr-TR");
        request.setBodyTemplate("Merhaba {{firstName}}");

        assertThatThrownBy(() -> notificationTemplateService.createTemplate(request))
                .isInstanceOf(DuplicateNotificationTemplateException.class);
        verify(notificationTemplateRepository, never()).save(any());
    }

    @Test
    void getTemplate_whenMissing_throwsNotificationTemplateNotFoundException() {
        UUID channelId = UUID.randomUUID();
        when(notificationTemplateRepository.findByCodeAndChannelIdAndLocale("GHOST", channelId, "tr-TR"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationTemplateService.getTemplate("GHOST", channelId, "tr-TR"))
                .isInstanceOf(NotificationTemplateNotFoundException.class);
    }

    private Channel channelWith(String code) {
        Channel channel = new Channel();
        channel.setId(UUID.randomUUID());
        channel.setCode(code);
        return channel;
    }
}
