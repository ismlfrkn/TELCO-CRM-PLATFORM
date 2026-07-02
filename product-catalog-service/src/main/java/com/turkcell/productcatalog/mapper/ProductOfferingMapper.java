package com.turkcell.productcatalog.mapper;

import com.turkcell.productcatalog.dto.response.ProductOfferingResponse;
import com.turkcell.productcatalog.entity.ProductOffering;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TariffMapper.class})
public interface ProductOfferingMapper {
    ProductOfferingResponse toResponse(ProductOffering productOffering);
}
