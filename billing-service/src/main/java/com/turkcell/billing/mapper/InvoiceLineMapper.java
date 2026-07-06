package com.turkcell.billing.mapper;

import com.turkcell.billing.dto.response.InvoiceLineResponse;
import com.turkcell.billing.entity.InvoiceLine;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceLineMapper {
    InvoiceLineResponse toResponse(InvoiceLine invoiceLine);
}
