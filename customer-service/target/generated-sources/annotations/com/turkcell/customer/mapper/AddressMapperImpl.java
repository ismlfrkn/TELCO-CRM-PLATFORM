package com.turkcell.customer.mapper;

import com.turkcell.customer.dto.response.AddressResponse;
import com.turkcell.customer.entity.Address;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-05T18:09:16+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class AddressMapperImpl implements AddressMapper {

    @Override
    public AddressResponse toResponse(Address address) {
        if ( address == null ) {
            return null;
        }

        AddressResponse addressResponse = new AddressResponse();

        addressResponse.setId( address.getId() );
        addressResponse.setLine1( address.getLine1() );
        addressResponse.setCity( address.getCity() );
        addressResponse.setDistrict( address.getDistrict() );
        addressResponse.setPostalCode( address.getPostalCode() );
        addressResponse.setDefault( address.isDefault() );

        return addressResponse;
    }
}
