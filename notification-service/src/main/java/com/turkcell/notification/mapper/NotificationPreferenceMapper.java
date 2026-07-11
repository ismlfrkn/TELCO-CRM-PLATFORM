package com.turkcell.notification.mapper;

import com.turkcell.notification.dto.response.NotificationPreferenceResponse;
import com.turkcell.notification.entity.NotificationPreference;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationPreferenceMapper {
    NotificationPreferenceResponse toResponse(NotificationPreference preference);
}
