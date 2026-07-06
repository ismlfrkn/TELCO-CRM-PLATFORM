package com.turkcell.subscription.mapper;

import com.turkcell.subscription.dto.response.SubscriptionResponse;
import com.turkcell.subscription.entity.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    SubscriptionResponse toResponse(Subscription subscription);
}
