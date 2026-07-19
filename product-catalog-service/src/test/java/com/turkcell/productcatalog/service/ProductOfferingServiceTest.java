package com.turkcell.productcatalog.service;

import com.turkcell.productcatalog.dto.request.ProductOfferingCreateRequest;
import com.turkcell.productcatalog.dto.request.ProductOfferingPatchRequest;
import com.turkcell.productcatalog.dto.request.ProductOfferingUpdateRequest;
import com.turkcell.productcatalog.dto.response.ProductOfferingResponse;
import com.turkcell.productcatalog.entity.ProductOffering;
import com.turkcell.productcatalog.entity.Tariff;
import com.turkcell.productcatalog.exception.ProductOfferingNotFoundException;
import com.turkcell.productcatalog.exception.TariffNotFoundException;
import com.turkcell.productcatalog.mapper.ProductOfferingMapper;
import com.turkcell.productcatalog.mapper.TariffMapper;
import com.turkcell.productcatalog.repository.ProductOfferingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductOfferingServiceTest {

    private ProductOfferingRepository productOfferingRepository;
    private TariffService tariffService;
    private OutboxEventService outboxEventService;
    private ProductOfferingService productOfferingService;

    @BeforeEach
    void setUp() {
        productOfferingRepository = mock(ProductOfferingRepository.class);
        tariffService = mock(TariffService.class);
        outboxEventService = mock(OutboxEventService.class);
        ProductOfferingMapper mapper = Mappers.getMapper(ProductOfferingMapper.class);

        ReflectionTestUtils.setField(mapper, "tariffMapper", Mappers.getMapper(TariffMapper.class));
        productOfferingService = new ProductOfferingService(productOfferingRepository, tariffService, mapper,
                outboxEventService);

        when(productOfferingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createProductOffering_resolvesTariffAndSaves() {
        Tariff tariff = existingTariff();
        when(tariffService.getTariffByCode("TRF-001")).thenReturn(tariff);

        ProductOfferingCreateRequest request = new ProductOfferingCreateRequest();
        request.setCode("PO-001");
        request.setName("Super Bundle");
        request.setTariffCode("TRF-001");
        request.setStatus("ACTIVE");
        request.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        ProductOfferingResponse response = productOfferingService.createProductOffering(request);

        assertThat(response.getCode()).isEqualTo("PO-001");
        verify(outboxEventService).publish(eq("ProductOffering"), any(), eq("ProductOfferingCreated"), any());
    }

    @Test
    void createProductOffering_whenTariffMissing_propagatesTariffNotFoundException() {
        when(tariffService.getTariffByCode("GHOST"))
                .thenThrow(new TariffNotFoundException("Tariff not found with code: GHOST"));

        ProductOfferingCreateRequest request = new ProductOfferingCreateRequest();
        request.setCode("PO-001");
        request.setName("Super Bundle");
        request.setTariffCode("GHOST");
        request.setStatus("ACTIVE");

        assertThatThrownBy(() -> productOfferingService.createProductOffering(request))
                .isInstanceOf(TariffNotFoundException.class);
        verify(productOfferingRepository, never()).save(any());
    }

    @Test
    void updateProductOffering_resolvesNewTariffAndReplacesFields() {
        ProductOffering offering = existingOffering();
        Tariff newTariff = existingTariff();
        newTariff.setCode("TRF-002");

        when(productOfferingRepository.findByCodeAndStatusNot("PO-001", "INACTIVE")).thenReturn(Optional.of(offering));
        when(tariffService.getTariffByCode("TRF-002")).thenReturn(newTariff);

        ProductOfferingUpdateRequest request = new ProductOfferingUpdateRequest();
        request.setName("Renamed Bundle");
        request.setTariffCode("TRF-002");
        request.setStatus("ACTIVE");

        ProductOfferingResponse response = productOfferingService.updateProductOffering("PO-001", request);

        assertThat(response.getName()).isEqualTo("Renamed Bundle");
        assertThat(offering.getTariff().getCode()).isEqualTo("TRF-002");
        verify(outboxEventService).publish(eq("ProductOffering"), eq(offering.getId()), eq("ProductOfferingUpdated"), any());
    }

    @Test
    void patchProductOffering_withoutTariffCode_doesNotResolveTariff() {
        ProductOffering offering = existingOffering();
        when(productOfferingRepository.findByCodeAndStatusNot("PO-001", "INACTIVE")).thenReturn(Optional.of(offering));

        ProductOfferingPatchRequest request = new ProductOfferingPatchRequest();
        request.setName("Patched Name");

        ProductOfferingResponse response = productOfferingService.patchProductOffering("PO-001", request);

        assertThat(response.getName()).isEqualTo("Patched Name");
        verifyNoInteractions(tariffService);
        verify(outboxEventService).publish(eq("ProductOffering"), eq(offering.getId()), eq("ProductOfferingUpdated"), any());
    }

    @Test
    void deleteProductOffering_softDeletesByMarkingInactive() {
        ProductOffering offering = existingOffering();
        when(productOfferingRepository.findByCodeAndStatusNot("PO-001", "INACTIVE")).thenReturn(Optional.of(offering));

        productOfferingService.deleteProductOffering("PO-001");

        assertThat(offering.getStatus()).isEqualTo("INACTIVE");
        verify(outboxEventService).publish(eq("ProductOffering"), eq(offering.getId()), eq("ProductOfferingDeactivated"), any());
    }

    @Test
    void getProductOfferingByCode_whenMissing_throwsProductOfferingNotFoundException() {
        when(productOfferingRepository.findByCodeAndStatusNot("GHOST", "INACTIVE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productOfferingService.getProductOfferingByCode("GHOST"))
                .isInstanceOf(ProductOfferingNotFoundException.class);
    }

    private Tariff existingTariff() {
        Tariff tariff = new Tariff();
        tariff.setId(UUID.randomUUID());
        tariff.setCode("TRF-001");
        tariff.setName("Super Tariff");
        return tariff;
    }

    private ProductOffering existingOffering() {
        ProductOffering offering = new ProductOffering();
        offering.setId(UUID.randomUUID());
        offering.setCode("PO-001");
        offering.setName("Super Bundle");
        offering.setTariff(existingTariff());
        offering.setStatus("ACTIVE");
        offering.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        return offering;
    }
}
