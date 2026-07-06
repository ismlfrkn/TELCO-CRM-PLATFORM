package com.turkcell.payment.mapper;

import com.turkcell.payment.dto.response.WalletResponse;
import com.turkcell.payment.entity.Wallet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletMapper {
    WalletResponse toResponse(Wallet wallet);
}
