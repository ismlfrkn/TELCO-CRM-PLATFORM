package com.turkcell.customer.mapper;

import com.turkcell.customer.dto.response.AddressResponse;
import com.turkcell.customer.entity.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressResponse toResponse(Address address);
}
