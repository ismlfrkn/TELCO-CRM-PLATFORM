package com.turkcell.customer.mapper;

import com.turkcell.customer.dto.response.DocumentResponse;
import com.turkcell.customer.entity.Document;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    DocumentResponse toResponse(Document document);
}
