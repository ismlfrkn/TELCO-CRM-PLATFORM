package com.turkcell.notification.service;

import com.turkcell.notification.dto.response.NotificationPreferenceResponse;
import com.turkcell.notification.entity.Channel;
import com.turkcell.notification.entity.NotificationPreference;
import com.turkcell.notification.mapper.NotificationPreferenceMapper;
import com.turkcell.notification.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * FR-30: kullanici bazli kanal opt-in/opt-out tercihleri. Kayit yoksa varsayilan opted-in
 * kabul edilir - musteri hicbir tercih belirtmeden mevcut senaryolar (welcome SMS vb.) calismaya
 * devam eder, sadece acikca opt-out eden kullanicilar icin gonderim durur.
 */
@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ChannelService channelService;
    private final NotificationPreferenceMapper notificationPreferenceMapper;

    public NotificationPreferenceService(NotificationPreferenceRepository notificationPreferenceRepository,
                                          ChannelService channelService,
                                          NotificationPreferenceMapper notificationPreferenceMapper) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.channelService = channelService;
        this.notificationPreferenceMapper = notificationPreferenceMapper;
    }

    public boolean isOptedIn(UUID userId, UUID channelId) {
        return notificationPreferenceRepository.findByUserIdAndChannelId(userId, channelId)
                .map(NotificationPreference::isOptedIn)
                .orElse(true);
    }

    @Transactional
    public NotificationPreferenceResponse setPreference(UUID userId, String channelCode, boolean optedIn) {
        Channel channel = channelService.getChannelByCode(channelCode);
        NotificationPreference preference = notificationPreferenceRepository
                .findByUserIdAndChannelId(userId, channel.getId())
                .orElseGet(NotificationPreference::new);

        preference.setUserId(userId);
        preference.setChannelId(channel.getId());
        preference.setOptedIn(optedIn);

        preference = notificationPreferenceRepository.save(preference);
        return notificationPreferenceMapper.toResponse(preference);
    }

    public List<NotificationPreferenceResponse> getPreferences(UUID userId) {
        return notificationPreferenceRepository.findAllByUserId(userId).stream()
                .map(notificationPreferenceMapper::toResponse)
                .toList();
    }
}
