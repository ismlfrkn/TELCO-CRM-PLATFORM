package com.turkcell.productcatalog.mapper;

import com.turkcell.productcatalog.dto.response.AddonResponse;
import com.turkcell.productcatalog.entity.Addon;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-02T10:59:58+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class AddonMapperImpl implements AddonMapper {

    @Override
    public AddonResponse toResponse(Addon addon) {
        if ( addon == null ) {
            return null;
        }

        AddonResponse addonResponse = new AddonResponse();

        addonResponse.setId( addon.getId() );
        addonResponse.setCode( addon.getCode() );
        addonResponse.setName( addon.getName() );
        addonResponse.setPrice( addon.getPrice() );
        addonResponse.setType( addon.getType() );
        addonResponse.setValidityDays( addon.getValidityDays() );
        addonResponse.setCurrency( addon.getCurrency() );
        addonResponse.setStatus( addon.getStatus() );
        addonResponse.setCreatedAt( addon.getCreatedAt() );

        return addonResponse;
    }
}
