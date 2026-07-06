package com.turkcell.notification.mapper;

import com.turkcell.notification.dto.response.NotificationResponse;
import com.turkcell.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
