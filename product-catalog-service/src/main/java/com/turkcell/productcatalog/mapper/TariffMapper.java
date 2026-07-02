package com.turkcell.productcatalog.mapper;

import com.turkcell.productcatalog.dto.response.TariffResponse;
import com.turkcell.productcatalog.entity.Tariff;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {AddonMapper.class})
public interface TariffMapper {
    TariffResponse toResponse(Tariff tariff);
}
