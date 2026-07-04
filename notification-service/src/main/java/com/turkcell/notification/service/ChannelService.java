package com.turkcell.notification.service;

import com.turkcell.notification.dto.request.ChannelCreateRequest;
import com.turkcell.notification.dto.response.ChannelResponse;
import com.turkcell.notification.entity.Channel;
import com.turkcell.notification.exception.ChannelNotFoundException;
import com.turkcell.notification.exception.DuplicateChannelCodeException;
import com.turkcell.notification.mapper.ChannelMapper;
import com.turkcell.notification.repository.ChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMapper channelMapper;

    public ChannelService(ChannelRepository channelRepository, ChannelMapper channelMapper) {
        this.channelRepository = channelRepository;
        this.channelMapper = channelMapper;
    }

    @Transactional
    public ChannelResponse createChannel(ChannelCreateRequest request) {
        if (channelRepository.existsByCode(request.getCode())) {
            throw new DuplicateChannelCodeException("Channel already exists with code: " + request.getCode());
        }

        Channel channel = new Channel();
        channel.setCode(request.getCode());
        channel.setDescription(request.getDescription());
        return channelMapper.toResponse(channelRepository.save(channel));
    }

    public Channel getChannelByCode(String code) {
        return channelRepository.findByCode(code)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found with code: " + code));
    }
}
