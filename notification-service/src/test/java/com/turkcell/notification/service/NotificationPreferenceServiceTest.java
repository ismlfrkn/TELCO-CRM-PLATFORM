package com.turkcell.notification.service;

import com.turkcell.notification.dto.response.NotificationPreferenceResponse;
import com.turkcell.notification.entity.Channel;
import com.turkcell.notification.entity.NotificationPreference;
import com.turkcell.notification.mapper.NotificationPreferenceMapper;
import com.turkcell.notification.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationPreferenceServiceTest {

    private NotificationPreferenceRepository notificationPreferenceRepository;
    private ChannelService channelService;
    private NotificationPreferenceService notificationPreferenceService;

    @BeforeEach
    void setUp() {
        notificationPreferenceRepository = mock(NotificationPreferenceRepository.class);
        channelService = mock(ChannelService.class);
        NotificationPreferenceMapper mapper = Mappers.getMapper(NotificationPreferenceMapper.class);

        notificationPreferenceService = new NotificationPreferenceService(
                notificationPreferenceRepository, channelService, mapper);

        when(notificationPreferenceRepository.save(any())).thenAnswer(inv -> {
            NotificationPreference preference = inv.getArgument(0);
            if (preference.getId() == null) {
                preference.setId(UUID.randomUUID());
            }
            return preference;
        });
    }

    @Test
    void isOptedIn_whenNoPreferenceRecorded_defaultsToTrue() {
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserIdAndChannelId(userId, channelId))
                .thenReturn(Optional.empty());

        assertThat(notificationPreferenceService.isOptedIn(userId, channelId)).isTrue();
    }

    @Test
    void isOptedIn_whenExplicitlyOptedOut_returnsFalse() {
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserIdAndChannelId(userId, channelId))
                .thenReturn(Optional.of(preferenceOf(userId, channelId, false)));

        assertThat(notificationPreferenceService.isOptedIn(userId, channelId)).isFalse();
    }

    @Test
    void isOptedIn_whenExplicitlyOptedIn_returnsTrue() {
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserIdAndChannelId(userId, channelId))
                .thenReturn(Optional.of(preferenceOf(userId, channelId, true)));

        assertThat(notificationPreferenceService.isOptedIn(userId, channelId)).isTrue();
    }

    @Test
    void setPreference_withNewUserChannelPair_createsPreference() {
        UUID userId = UUID.randomUUID();
        Channel channel = channelWith("SMS");
        when(channelService.getChannelByCode("SMS")).thenReturn(channel);
        when(notificationPreferenceRepository.findByUserIdAndChannelId(userId, channel.getId()))
                .thenReturn(Optional.empty());

        NotificationPreferenceResponse response = notificationPreferenceService.setPreference(userId, "SMS", false);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getChannelId()).isEqualTo(channel.getId());
        assertThat(response.isOptedIn()).isFalse();
    }

    @Test
    void setPreference_withExistingPair_updatesExistingRowInsteadOfCreatingDuplicate() {
        UUID userId = UUID.randomUUID();
        Channel channel = channelWith("EMAIL");
        NotificationPreference existing = preferenceOf(userId, channel.getId(), true);
        when(channelService.getChannelByCode("EMAIL")).thenReturn(channel);
        when(notificationPreferenceRepository.findByUserIdAndChannelId(userId, channel.getId()))
                .thenReturn(Optional.of(existing));

        NotificationPreferenceResponse response = notificationPreferenceService.setPreference(userId, "EMAIL", false);

        assertThat(response.getId()).isEqualTo(existing.getId());
        assertThat(response.isOptedIn()).isFalse();
        verify(notificationPreferenceRepository, times(1)).save(existing);
    }

    @Test
    void getPreferences_returnsAllMappedPreferencesForUser() {
        UUID userId = UUID.randomUUID();
        NotificationPreference smsPref = preferenceOf(userId, UUID.randomUUID(), false);
        NotificationPreference emailPref = preferenceOf(userId, UUID.randomUUID(), true);
        when(notificationPreferenceRepository.findAllByUserId(userId)).thenReturn(List.of(smsPref, emailPref));

        List<NotificationPreferenceResponse> responses = notificationPreferenceService.getPreferences(userId);

        assertThat(responses).hasSize(2);
    }

    private Channel channelWith(String code) {
        Channel channel = new Channel();
        channel.setId(UUID.randomUUID());
        channel.setCode(code);
        return channel;
    }

    private NotificationPreference preferenceOf(UUID userId, UUID channelId, boolean optedIn) {
        NotificationPreference preference = new NotificationPreference();
        preference.setId(UUID.randomUUID());
        preference.setUserId(userId);
        preference.setChannelId(channelId);
        preference.setOptedIn(optedIn);
        return preference;
    }
}
