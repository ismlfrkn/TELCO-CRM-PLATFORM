package com.turkcell.productcatalog.service;

import com.turkcell.productcatalog.dto.request.TariffCreateRequest;
import com.turkcell.productcatalog.dto.request.TariffPatchRequest;
import com.turkcell.productcatalog.dto.request.TariffUpdateRequest;
import com.turkcell.productcatalog.dto.response.TariffResponse;
import com.turkcell.productcatalog.entity.Addon;
import com.turkcell.productcatalog.entity.Tariff;
import com.turkcell.productcatalog.exception.TariffNotFoundException;
import com.turkcell.productcatalog.mapper.TariffMapper;
import com.turkcell.productcatalog.repository.TariffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TariffServiceTest {

    private TariffRepository tariffRepository;
    private AddonService addonService;
    private OutboxEventService outboxEventService;
    private TariffService tariffService;

    @BeforeEach
    void setUp() {
        tariffRepository = mock(TariffRepository.class);
        addonService = mock(AddonService.class);
        outboxEventService = mock(OutboxEventService.class);
        TariffMapper tariffMapper = Mappers.getMapper(TariffMapper.class);
        tariffService = new TariffService(tariffRepository, addonService, tariffMapper, outboxEventService);

        when(tariffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createTariff_savesWithGivenFields() {
        TariffCreateRequest request = new TariffCreateRequest();
        request.setCode("TRF-001");
        request.setName("Super Tariff");
        request.setType("POSTPAID");
        request.setMonthlyFee(new BigDecimal("150.00"));
        request.setMinutesIncluded(1000);
        request.setSmsIncluded(1000);
        request.setDataMbIncluded(10240);
        request.setStatus("ACTIVE");
        request.setCurrency("TRY");
        request.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        TariffResponse response = tariffService.createTariff(request);

        assertThat(response.getCode()).isEqualTo("TRF-001");
        assertThat(response.getMonthlyFee()).isEqualByComparingTo("150.00");
        verify(outboxEventService).publish(eq("Tariff"), any(), eq("TariffCreated"), any());
    }

    @Test
    void getTariffByCode_whenMissingOrInactive_throwsTariffNotFoundException() {
        when(tariffRepository.findByCodeAndStatusNot("GHOST", "INACTIVE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.getTariffByCode("GHOST"))
                .isInstanceOf(TariffNotFoundException.class);
    }

    @Test
    void updateTariff_replacesEditableFields() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));

        TariffUpdateRequest request = new TariffUpdateRequest();
        request.setName("Renamed Tariff");
        request.setType("POSTPAID");
        request.setMonthlyFee(new BigDecimal("200.00"));
        request.setStatus("ACTIVE");
        request.setCurrency("TRY");

        TariffResponse response = tariffService.updateTariff("TRF-001", request);

        assertThat(response.getName()).isEqualTo("Renamed Tariff");
        assertThat(response.getMonthlyFee()).isEqualByComparingTo("200.00");
        verify(outboxEventService).publish(eq("Tariff"), eq(tariff.getId()), eq("TariffUpdated"), any());
    }

    @Test
    void patchTariff_onlyChangesProvidedFields() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));

        TariffPatchRequest request = new TariffPatchRequest();
        request.setName("Patched Name");

        TariffResponse response = tariffService.patchTariff("TRF-001", request);

        assertThat(response.getName()).isEqualTo("Patched Name");
        assertThat(response.getMonthlyFee()).isEqualByComparingTo("150.00"); // degismedi
        verify(outboxEventService).publish(eq("Tariff"), eq(tariff.getId()), eq("TariffUpdated"), any());
    }

    @Test
    void deleteTariff_softDeletesByMarkingInactive() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));

        tariffService.deleteTariff("TRF-001");

        assertThat(tariff.getStatus()).isEqualTo("INACTIVE");
        verify(outboxEventService).publish(eq("Tariff"), eq(tariff.getId()), eq("TariffDeactivated"), any());
    }

    @Test
    void linkAddonToTariff_addsAddonToTariffsAddonSet() {
        Tariff tariff = existingTariff();
        Addon addon = new Addon();
        addon.setId(UUID.randomUUID());
        addon.setCode("ADN-001");

        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));
        when(addonService.getAddonByCode("ADN-001")).thenReturn(addon);

        tariffService.linkAddonToTariff("TRF-001", "ADN-001");

        assertThat(tariff.getAddons()).contains(addon);
    }

    private Tariff existingTariff() {
        Tariff tariff = new Tariff();
        tariff.setId(UUID.randomUUID());
        tariff.setCode("TRF-001");
        tariff.setName("Super Tariff");
        tariff.setType("POSTPAID");
        tariff.setMonthlyFee(new BigDecimal("150.00"));
        tariff.setStatus("ACTIVE");
        tariff.setCurrency("TRY");
        tariff.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        return tariff;
    }
}
