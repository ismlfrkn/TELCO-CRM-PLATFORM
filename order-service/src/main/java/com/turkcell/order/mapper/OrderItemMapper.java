package com.turkcell.order.mapper;

import com.turkcell.order.dto.response.OrderItemResponse;
import com.turkcell.order.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    OrderItemResponse toResponse(OrderItem orderItem);
}
