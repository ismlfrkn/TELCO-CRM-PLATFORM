package com.turkcell.billing.mapper;

import com.turkcell.billing.dto.response.BillCycleResponse;
import com.turkcell.billing.entity.BillCycle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BillCycleMapper {
    BillCycleResponse toResponse(BillCycle billCycle);
}
