package com.turkcell.order.mapper;

import com.turkcell.order.dto.response.OrderResponse;
import com.turkcell.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "items", ignore = true)
    OrderResponse toResponse(Order order);
}
