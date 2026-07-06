package com.turkcell.subscription.mapper;

import com.turkcell.subscription.dto.response.MsisdnPoolResponse;
import com.turkcell.subscription.entity.MsisdnPool;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MsisdnPoolMapper {
    MsisdnPoolResponse toResponse(MsisdnPool msisdnPool);
}
