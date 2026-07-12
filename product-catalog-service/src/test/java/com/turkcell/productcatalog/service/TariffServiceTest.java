package com.turkcell.productcatalog.service;

import com.turkcell.productcatalog.dto.request.TariffCreateRequest;
import com.turkcell.productcatalog.dto.request.TariffPatchRequest;
import com.turkcell.productcatalog.dto.request.TariffUpdateRequest;
import com.turkcell.productcatalog.dto.response.TariffResponse;
import com.turkcell.productcatalog.dto.response.TariffVersionResponse;
import com.turkcell.productcatalog.entity.Addon;
import com.turkcell.productcatalog.entity.Tariff;
import com.turkcell.productcatalog.entity.TariffVersion;
import com.turkcell.productcatalog.exception.TariffNotFoundException;
import com.turkcell.productcatalog.exception.TariffVersionNotFoundException;
import com.turkcell.productcatalog.mapper.TariffMapper;
import com.turkcell.productcatalog.mapper.TariffVersionMapper;
import com.turkcell.productcatalog.repository.TariffRepository;
import com.turkcell.productcatalog.repository.TariffVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TariffServiceTest {

    private TariffRepository tariffRepository;
    private TariffVersionRepository tariffVersionRepository;
    private AddonService addonService;
    private OutboxEventService outboxEventService;
    private TariffService tariffService;

    @BeforeEach
    void setUp() {
        tariffRepository = mock(TariffRepository.class);
        tariffVersionRepository = mock(TariffVersionRepository.class);
        addonService = mock(AddonService.class);
        outboxEventService = mock(OutboxEventService.class);
        TariffMapper tariffMapper = Mappers.getMapper(TariffMapper.class);
        TariffVersionMapper tariffVersionMapper = Mappers.getMapper(TariffVersionMapper.class);
        tariffService = new TariffService(tariffRepository, tariffVersionRepository, addonService, tariffMapper,
                tariffVersionMapper, outboxEventService);

        when(tariffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tariffVersionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
    void createTariff_withoutOverageRates_defaultsToZero() {
        TariffCreateRequest request = new TariffCreateRequest();
        request.setCode("TRF-002");
        request.setName("No Overage Tariff");
        request.setType("POSTPAID");
        request.setMonthlyFee(new BigDecimal("100.00"));
        request.setStatus("ACTIVE");
        request.setCurrency("TRY");

        TariffResponse response = tariffService.createTariff(request);

        assertThat(response.getOverageRatePerMinute()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getOverageRateSms()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getOverageRatePerMb()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createTariff_withOverageRates_persistsThem() {
        TariffCreateRequest request = new TariffCreateRequest();
        request.setCode("TRF-003");
        request.setName("Overage Priced Tariff");
        request.setType("POSTPAID");
        request.setMonthlyFee(new BigDecimal("100.00"));
        request.setStatus("ACTIVE");
        request.setCurrency("TRY");
        request.setOverageRatePerMinute(new BigDecimal("0.50"));
        request.setOverageRateSms(new BigDecimal("0.10"));
        request.setOverageRatePerMb(new BigDecimal("0.05"));

        TariffResponse response = tariffService.createTariff(request);

        assertThat(response.getOverageRatePerMinute()).isEqualByComparingTo("0.50");
        assertThat(response.getOverageRateSms()).isEqualByComparingTo("0.10");
        assertThat(response.getOverageRatePerMb()).isEqualByComparingTo("0.05");
    }

    @Test
    void getTariffByCode_whenMissingOrInactive_throwsTariffNotFoundException() {
        when(tariffRepository.findByCodeAndStatusNot("GHOST", "INACTIVE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.getTariffByCode("GHOST"))
                .isInstanceOf(TariffNotFoundException.class);
    }

    @Test
    void updateTariff_whenMonthlyFeeChanges_publishesTariffPriceChanged() {
        Tariff tariff = existingTariff(); // monthlyFee = 150.00
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
        verify(outboxEventService).publish(eq("Tariff"), eq(tariff.getId()), eq("TariffPriceChanged"), any());
        verify(outboxEventService, never()).publish(eq("Tariff"), any(), eq("TariffUpdated"), any());
    }

    @Test
    void updateTariff_whenMonthlyFeeUnchanged_publishesTariffUpdated() {
        Tariff tariff = existingTariff(); // monthlyFee = 150.00
        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));

        TariffUpdateRequest request = new TariffUpdateRequest();
        request.setName("Renamed Tariff");
        request.setType("POSTPAID");
        request.setMonthlyFee(new BigDecimal("150.00")); // ayni fiyat
        request.setStatus("ACTIVE");
        request.setCurrency("TRY");

        tariffService.updateTariff("TRF-001", request);

        verify(outboxEventService).publish(eq("Tariff"), eq(tariff.getId()), eq("TariffUpdated"), any());
        verify(outboxEventService, never()).publish(eq("Tariff"), any(), eq("TariffPriceChanged"), any());
    }

    @Test
    void patchTariff_whenMonthlyFeeChanges_publishesTariffPriceChanged() {
        Tariff tariff = existingTariff(); // monthlyFee = 150.00
        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));

        TariffPatchRequest request = new TariffPatchRequest();
        request.setMonthlyFee(new BigDecimal("175.00"));

        tariffService.patchTariff("TRF-001", request);

        verify(outboxEventService).publish(eq("Tariff"), eq(tariff.getId()), eq("TariffPriceChanged"), any());
        verify(outboxEventService, never()).publish(eq("Tariff"), any(), eq("TariffUpdated"), any());
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
    void patchTariff_withOverageRatePerMinute_onlyUpdatesThatRate() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));

        TariffPatchRequest request = new TariffPatchRequest();
        request.setOverageRatePerMinute(new BigDecimal("0.50"));

        TariffResponse response = tariffService.patchTariff("TRF-001", request);

        assertThat(response.getOverageRatePerMinute()).isEqualByComparingTo("0.50");
        assertThat(response.getOverageRateSms()).isEqualByComparingTo(BigDecimal.ZERO); // degismedi
        assertThat(response.getOverageRatePerMb()).isEqualByComparingTo(BigDecimal.ZERO); // degismedi
        assertThat(response.getName()).isEqualTo("Super Tariff"); // degismedi
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

    @Test
    void updateTariff_archivesPreviousVersionAndIncrementsVersion() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));

        TariffUpdateRequest request = new TariffUpdateRequest();
        request.setName("Renamed Tariff");
        request.setType("POSTPAID");
        request.setMonthlyFee(new BigDecimal("200.00"));
        request.setStatus("ACTIVE");
        request.setCurrency("TRY");

        TariffResponse response = tariffService.updateTariff("TRF-001", request);

        assertThat(response.getVersion()).isEqualTo(2);
        ArgumentCaptor<TariffVersion> captor = ArgumentCaptor.forClass(TariffVersion.class);
        verify(tariffVersionRepository).save(captor.capture());
        TariffVersion archived = captor.getValue();
        assertThat(archived.getVersion()).isEqualTo(1);
        assertThat(archived.getName()).isEqualTo("Super Tariff"); // guncellemeden ONCEKI isim
        assertThat(archived.getMonthlyFee()).isEqualByComparingTo("150.00"); // guncellemeden ONCEKI ucret
    }

    @Test
    void patchTariff_archivesPreviousVersionAndIncrementsVersion() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCodeAndStatusNot("TRF-001", "INACTIVE")).thenReturn(Optional.of(tariff));

        TariffPatchRequest request = new TariffPatchRequest();
        request.setName("Patched Name");

        TariffResponse response = tariffService.patchTariff("TRF-001", request);

        assertThat(response.getVersion()).isEqualTo(2);
        verify(tariffVersionRepository).save(any());
    }

    @Test
    void getTariffVersion_whenRequestingCurrentVersion_returnsLiveTariffTerms() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCode("TRF-001")).thenReturn(Optional.of(tariff));

        TariffVersionResponse response = tariffService.getTariffVersion("TRF-001", 1);

        assertThat(response.getMonthlyFee()).isEqualByComparingTo("150.00");
        assertThat(response.getSupersededAt()).isNull();
    }

    @Test
    void getTariffVersion_whenRequestingArchivedVersion_returnsHistoricalTerms() {
        Tariff tariff = existingTariff();
        tariff.setVersion(2);
        tariff.setMonthlyFee(new BigDecimal("200.00"));
        when(tariffRepository.findByCode("TRF-001")).thenReturn(Optional.of(tariff));

        TariffVersion archived = new TariffVersion();
        archived.setCode("TRF-001");
        archived.setVersion(1);
        archived.setMonthlyFee(new BigDecimal("150.00"));
        when(tariffVersionRepository.findByCodeAndVersion("TRF-001", 1)).thenReturn(Optional.of(archived));

        TariffVersionResponse response = tariffService.getTariffVersion("TRF-001", 1);

        assertThat(response.getMonthlyFee()).isEqualByComparingTo("150.00");
    }

    @Test
    void getTariffVersion_whenVersionNeverExisted_throwsTariffVersionNotFoundException() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCode("TRF-001")).thenReturn(Optional.of(tariff));
        when(tariffVersionRepository.findByCodeAndVersion("TRF-001", 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.getTariffVersion("TRF-001", 99))
                .isInstanceOf(TariffVersionNotFoundException.class);
    }

    @Test
    void getTariffVersion_whenTariffCodeUnknown_throwsTariffNotFoundException() {
        when(tariffRepository.findByCode("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tariffService.getTariffVersion("GHOST", 1))
                .isInstanceOf(TariffNotFoundException.class);
    }

    @Test
    void getTariffVersionHistory_returnsArchivedVersionsDescending() {
        Tariff tariff = existingTariff();
        when(tariffRepository.findByCode("TRF-001")).thenReturn(Optional.of(tariff));

        TariffVersion v2 = new TariffVersion();
        v2.setCode("TRF-001");
        v2.setVersion(2);
        TariffVersion v1 = new TariffVersion();
        v1.setCode("TRF-001");
        v1.setVersion(1);
        when(tariffVersionRepository.findAllByCodeOrderByVersionDesc("TRF-001")).thenReturn(List.of(v2, v1));

        List<TariffVersionResponse> history = tariffService.getTariffVersionHistory("TRF-001");

        assertThat(history).extracting(TariffVersionResponse::getVersion).containsExactly(2, 1);
    }

    private Tariff existingTariff() {
        Tariff tariff = new Tariff();
        tariff.setId(UUID.randomUUID());
        tariff.setCode("TRF-001");
        tariff.setVersion(1);
        tariff.setName("Super Tariff");
        tariff.setType("POSTPAID");
        tariff.setMonthlyFee(new BigDecimal("150.00"));
        tariff.setStatus("ACTIVE");
        tariff.setCurrency("TRY");
        tariff.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        return tariff;
    }
}
