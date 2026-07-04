package com.turkcell.notification.mapper;

import com.turkcell.notification.dto.response.ChannelResponse;
import com.turkcell.notification.entity.Channel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChannelMapper {
    ChannelResponse toResponse(Channel channel);
}
