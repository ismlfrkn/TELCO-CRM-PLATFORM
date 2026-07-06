package com.turkcell.notification.mapper;

import com.turkcell.notification.dto.response.NotificationTemplateResponse;
import com.turkcell.notification.entity.NotificationTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationTemplateMapper {
    NotificationTemplateResponse toResponse(NotificationTemplate template);
}
