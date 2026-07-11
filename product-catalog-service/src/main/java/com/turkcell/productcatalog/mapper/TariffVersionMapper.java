package com.turkcell.productcatalog.mapper;

import com.turkcell.productcatalog.dto.response.TariffVersionResponse;
import com.turkcell.productcatalog.entity.Tariff;
import com.turkcell.productcatalog.entity.TariffVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TariffVersionMapper {

    // Guncel (henuz arsivlenmemis) versiyon: supersededAt yok demektir.
    @Mapping(target = "supersededAt", ignore = true)
    TariffVersionResponse fromCurrent(Tariff tariff);

    TariffVersionResponse fromHistory(TariffVersion tariffVersion);
}
