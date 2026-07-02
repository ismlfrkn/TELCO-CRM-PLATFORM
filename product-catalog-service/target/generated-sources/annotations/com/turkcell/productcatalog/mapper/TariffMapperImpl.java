package com.turkcell.productcatalog.mapper;

import com.turkcell.productcatalog.dto.response.AddonResponse;
import com.turkcell.productcatalog.dto.response.TariffResponse;
import com.turkcell.productcatalog.entity.Addon;
import com.turkcell.productcatalog.entity.Tariff;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-02T10:59:57+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class TariffMapperImpl implements TariffMapper {

    @Autowired
    private AddonMapper addonMapper;

    @Override
    public TariffResponse toResponse(Tariff tariff) {
        if ( tariff == null ) {
            return null;
        }

        TariffResponse tariffResponse = new TariffResponse();

        tariffResponse.setId( tariff.getId() );
        tariffResponse.setCode( tariff.getCode() );
        tariffResponse.setName( tariff.getName() );
        tariffResponse.setType( tariff.getType() );
        tariffResponse.setMonthlyFee( tariff.getMonthlyFee() );
        tariffResponse.setMinutesIncluded( tariff.getMinutesIncluded() );
        tariffResponse.setSmsIncluded( tariff.getSmsIncluded() );
        tariffResponse.setDataMbIncluded( tariff.getDataMbIncluded() );
        tariffResponse.setStatus( tariff.getStatus() );
        tariffResponse.setEffectiveFrom( tariff.getEffectiveFrom() );
        tariffResponse.setEffectiveTo( tariff.getEffectiveTo() );
        tariffResponse.setCurrency( tariff.getCurrency() );
        tariffResponse.setCreatedAt( tariff.getCreatedAt() );
        tariffResponse.setAddons( addonSetToAddonResponseList( tariff.getAddons() ) );

        return tariffResponse;
    }

    protected List<AddonResponse> addonSetToAddonResponseList(Set<Addon> set) {
        if ( set == null ) {
            return null;
        }

        List<AddonResponse> list = new ArrayList<AddonResponse>( set.size() );
        for ( Addon addon : set ) {
            list.add( addonMapper.toResponse( addon ) );
        }

        return list;
    }
}
