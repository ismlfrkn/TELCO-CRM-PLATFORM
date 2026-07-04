package com.turkcell.payment.mapper;

import com.turkcell.payment.dto.response.PaymentResponse;
import com.turkcell.payment.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toResponse(Payment payment);
}
