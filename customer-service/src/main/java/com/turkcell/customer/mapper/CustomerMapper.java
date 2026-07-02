package com.turkcell.customer.mapper;

import com.turkcell.customer.dto.response.CustomerResponse;
import com.turkcell.customer.entity.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerResponse toResponse(Customer customer);
}
