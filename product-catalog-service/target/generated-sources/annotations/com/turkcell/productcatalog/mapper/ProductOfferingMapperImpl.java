package com.turkcell.productcatalog.mapper;

import com.turkcell.productcatalog.dto.response.ProductOfferingResponse;
import com.turkcell.productcatalog.entity.ProductOffering;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-02T10:59:58+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class ProductOfferingMapperImpl implements ProductOfferingMapper {

    @Autowired
    private TariffMapper tariffMapper;

    @Override
    public ProductOfferingResponse toResponse(ProductOffering productOffering) {
        if ( productOffering == null ) {
            return null;
        }

        ProductOfferingResponse productOfferingResponse = new ProductOfferingResponse();

        productOfferingResponse.setId( productOffering.getId() );
        productOfferingResponse.setCode( productOffering.getCode() );
        productOfferingResponse.setName( productOffering.getName() );
        productOfferingResponse.setDescription( productOffering.getDescription() );
        productOfferingResponse.setTariff( tariffMapper.toResponse( productOffering.getTariff() ) );
        productOfferingResponse.setStatus( productOffering.getStatus() );
        productOfferingResponse.setEffectiveFrom( productOffering.getEffectiveFrom() );
        productOfferingResponse.setEffectiveTo( productOffering.getEffectiveTo() );
        productOfferingResponse.setCreatedAt( productOffering.getCreatedAt() );

        return productOfferingResponse;
    }
}
