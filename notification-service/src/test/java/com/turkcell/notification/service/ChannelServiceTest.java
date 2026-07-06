package com.turkcell.notification.service;

import com.turkcell.notification.dto.request.ChannelCreateRequest;
import com.turkcell.notification.dto.response.ChannelResponse;
import com.turkcell.notification.entity.Channel;
import com.turkcell.notification.exception.ChannelNotFoundException;
import com.turkcell.notification.exception.DuplicateChannelCodeException;
import com.turkcell.notification.mapper.ChannelMapper;
import com.turkcell.notification.repository.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChannelServiceTest {

    private ChannelRepository channelRepository;
    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        channelRepository = mock(ChannelRepository.class);
        ChannelMapper channelMapper = Mappers.getMapper(ChannelMapper.class);
        channelService = new ChannelService(channelRepository, channelMapper);

        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createChannel_withNewCode_succeeds() {
        when(channelRepository.existsByCode("SMS")).thenReturn(false);

        ChannelCreateRequest request = new ChannelCreateRequest();
        request.setCode("SMS");
        request.setDescription("Short Message Service");

        ChannelResponse response = channelService.createChannel(request);

        assertThat(response.getCode()).isEqualTo("SMS");
    }

    @Test
    void createChannel_withDuplicateCode_throwsDuplicateChannelCodeException() {
        when(channelRepository.existsByCode("SMS")).thenReturn(true);

        ChannelCreateRequest request = new ChannelCreateRequest();
        request.setCode("SMS");

        assertThatThrownBy(() -> channelService.createChannel(request))
                .isInstanceOf(DuplicateChannelCodeException.class);
        verify(channelRepository, never()).save(any());
    }

    @Test
    void getChannelByCode_whenMissing_throwsChannelNotFoundException() {
        when(channelRepository.findByCode("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.getChannelByCode("GHOST"))
                .isInstanceOf(ChannelNotFoundException.class);
    }

    @Test
    void getChannelByCode_whenExists_returnsChannel() {
        Channel channel = new Channel();
        channel.setCode("EMAIL");
        when(channelRepository.findByCode("EMAIL")).thenReturn(Optional.of(channel));

        Channel result = channelService.getChannelByCode("EMAIL");

        assertThat(result.getCode()).isEqualTo("EMAIL");
    }
}
