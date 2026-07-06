package com.turkcell.subscription.mapper;

import com.turkcell.subscription.dto.response.SimCardResponse;
import com.turkcell.subscription.entity.SimCard;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SimCardMapper {
    SimCardResponse toResponse(SimCard simCard);
}
