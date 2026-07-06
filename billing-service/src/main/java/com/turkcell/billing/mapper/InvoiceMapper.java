package com.turkcell.billing.mapper;

import com.turkcell.billing.dto.response.InvoiceResponse;
import com.turkcell.billing.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "lines", ignore = true)
    InvoiceResponse toResponse(Invoice invoice);
}
