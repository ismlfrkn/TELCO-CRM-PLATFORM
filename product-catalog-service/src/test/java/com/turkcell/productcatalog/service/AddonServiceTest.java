package com.turkcell.productcatalog.service;

import com.turkcell.productcatalog.dto.request.AddonCreateRequest;
import com.turkcell.productcatalog.dto.request.AddonPatchRequest;
import com.turkcell.productcatalog.dto.response.AddonResponse;
import com.turkcell.productcatalog.entity.Addon;
import com.turkcell.productcatalog.exception.AddonNotFoundException;
import com.turkcell.productcatalog.mapper.AddonMapper;
import com.turkcell.productcatalog.repository.AddonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AddonServiceTest {

    private AddonRepository addonRepository;
    private OutboxEventService outboxEventService;
    private AddonService addonService;

    @BeforeEach
    void setUp() {
        addonRepository = mock(AddonRepository.class);
        outboxEventService = mock(OutboxEventService.class);
        AddonMapper addonMapper = Mappers.getMapper(AddonMapper.class);
        addonService = new AddonService(addonRepository, addonMapper, outboxEventService);

        when(addonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getAllAddons_withTariffCode_usesTariffFilteredQuery() {
        Pageable pageable = Pageable.unpaged();
        when(addonRepository.findAllByTariffCodeAndActive("TRF-001", pageable))
                .thenReturn(new PageImpl<>(List.of(existingAddon())));

        Page<AddonResponse> result = addonService.getAllAddons("TRF-001", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(addonRepository, never()).findAllByStatusNot(any(), any());
    }

    @Test
    void getAllAddons_withoutTariffCode_usesStatusNotQuery() {
        Pageable pageable = Pageable.unpaged();
        when(addonRepository.findAllByStatusNot("INACTIVE", pageable))
                .thenReturn(new PageImpl<>(List.of(existingAddon())));

        Page<AddonResponse> result = addonService.getAllAddons(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(addonRepository, never()).findAllByTariffCodeAndActive(any(), any());
    }

    @Test
    void getAddonByCode_whenMissingOrInactive_throwsAddonNotFoundException() {
        when(addonRepository.findByCodeAndStatusNot("GHOST", "INACTIVE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addonService.getAddonByCode("GHOST"))
                .isInstanceOf(AddonNotFoundException.class);
    }

    @Test
    void createAddon_savesWithGivenFields() {
        AddonCreateRequest request = new AddonCreateRequest();
        request.setCode("ADN-001");
        request.setName("Extra 5GB");
        request.setPrice(new BigDecimal("50.00"));
        request.setType("DATA");
        request.setValidityDays(30);
        request.setCurrency("TRY");
        request.setStatus("ACTIVE");

        AddonResponse response = addonService.createAddon(request);

        assertThat(response.getCode()).isEqualTo("ADN-001");
        assertThat(response.getPrice()).isEqualByComparingTo("50.00");
        verify(outboxEventService).publish(eq("Addon"), any(), eq("AddonCreated"), any());
    }

    @Test
    void patchAddon_onlyChangesProvidedFields() {
        Addon addon = existingAddon();
        when(addonRepository.findByCodeAndStatusNot("ADN-001", "INACTIVE")).thenReturn(Optional.of(addon));

        AddonPatchRequest request = new AddonPatchRequest();
        request.setPrice(new BigDecimal("75.00"));

        AddonResponse response = addonService.patchAddon("ADN-001", request);

        assertThat(response.getPrice()).isEqualByComparingTo("75.00");
        assertThat(response.getName()).isEqualTo("Extra 5GB");
        verify(outboxEventService).publish(eq("Addon"), eq(addon.getId()), eq("AddonUpdated"), any());
    }

    @Test
    void deleteAddon_softDeletesByMarkingInactive() {
        Addon addon = existingAddon();
        when(addonRepository.findByCodeAndStatusNot("ADN-001", "INACTIVE")).thenReturn(Optional.of(addon));

        addonService.deleteAddon("ADN-001");

        assertThat(addon.getStatus()).isEqualTo("INACTIVE");
        verify(outboxEventService).publish(eq("Addon"), eq(addon.getId()), eq("AddonDeactivated"), any());
    }

    private Addon existingAddon() {
        Addon addon = new Addon();
        addon.setId(UUID.randomUUID());
        addon.setCode("ADN-001");
        addon.setName("Extra 5GB");
        addon.setPrice(new BigDecimal("50.00"));
        addon.setType("DATA");
        addon.setValidityDays(30);
        addon.setCurrency("TRY");
        addon.setStatus("ACTIVE");
        return addon;
    }
}
