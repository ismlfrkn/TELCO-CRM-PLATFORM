package com.turkcell.customer.mapper;

import com.turkcell.customer.dto.response.CustomerResponse;
import com.turkcell.customer.entity.Customer;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-03T12:49:34+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class CustomerMapperImpl implements CustomerMapper {

    @Override
    public CustomerResponse toResponse(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        CustomerResponse customerResponse = new CustomerResponse();

        customerResponse.setId( customer.getId() );
        customerResponse.setType( customer.getType() );
        customerResponse.setFirstName( customer.getFirstName() );
        customerResponse.setLastName( customer.getLastName() );
        customerResponse.setIdentityNumber( customer.getIdentityNumber() );
        customerResponse.setDateOfBirth( customer.getDateOfBirth() );
        customerResponse.setEmail( customer.getEmail() );
        customerResponse.setPhone( customer.getPhone() );
        customerResponse.setStatus( customer.getStatus() );
        customerResponse.setCreatedAt( customer.getCreatedAt() );

        return customerResponse;
    }
}
