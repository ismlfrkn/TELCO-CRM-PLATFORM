package com.turkcell.productcatalog.service;

import com.turkcell.productcatalog.dto.request.ProductOfferingCreateRequest;
import com.turkcell.productcatalog.dto.request.ProductOfferingPatchRequest;
import com.turkcell.productcatalog.dto.request.ProductOfferingUpdateRequest;
import com.turkcell.productcatalog.dto.response.ProductOfferingResponse;
import com.turkcell.productcatalog.entity.ProductOffering;
import com.turkcell.productcatalog.entity.Tariff;
import com.turkcell.productcatalog.exception.ProductOfferingNotFoundException;
import com.turkcell.productcatalog.mapper.ProductOfferingMapper;
import com.turkcell.productcatalog.repository.ProductOfferingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductOfferingService {

    private static final String AGGREGATE_TYPE = "ProductOffering";

    private final ProductOfferingRepository productOfferingRepository;
    private final TariffService tariffService;
    private final ProductOfferingMapper productOfferingMapper;
    private final OutboxEventService outboxEventService;

    public ProductOfferingService(ProductOfferingRepository productOfferingRepository, TariffService tariffService,
                                   ProductOfferingMapper productOfferingMapper, OutboxEventService outboxEventService) {
        this.productOfferingRepository = productOfferingRepository;
        this.tariffService = tariffService;
        this.productOfferingMapper = productOfferingMapper;
        this.outboxEventService = outboxEventService;
    }

    public Page<ProductOfferingResponse> getAllProductOfferings(Pageable pageable) {
        return productOfferingRepository.findAllByStatusNot("INACTIVE", pageable)
                .map(productOfferingMapper::toResponse);
    }

    public ProductOfferingResponse getProductOfferingResponseByCode(String code) {
        return productOfferingMapper.toResponse(getProductOfferingByCode(code));
    }

    public ProductOffering getProductOfferingByCode(String code) {
        return productOfferingRepository.findByCodeAndStatusNot(code, "INACTIVE")
                .orElseThrow(() -> new ProductOfferingNotFoundException("Product Offering not found with code: " + code));
    }

    @Transactional
    public ProductOfferingResponse createProductOffering(ProductOfferingCreateRequest request) {
        Tariff tariff = tariffService.getTariffByCode(request.getTariffCode());

        ProductOffering offering = new ProductOffering();
        offering.setCode(request.getCode());
        offering.setName(request.getName());
        offering.setDescription(request.getDescription());
        offering.setTariff(tariff);
        offering.setStatus(request.getStatus());
        offering.setEffectiveFrom(request.getEffectiveFrom());
        offering.setEffectiveTo(request.getEffectiveTo());

        offering = productOfferingRepository.save(offering);
        ProductOfferingResponse response = productOfferingMapper.toResponse(offering);
        outboxEventService.publish(AGGREGATE_TYPE, offering.getId(), "ProductOfferingCreated", response);
        return response;
    }

    @Transactional
    public ProductOfferingResponse updateProductOffering(String code, ProductOfferingUpdateRequest request) {
        ProductOffering offering = getProductOfferingByCode(code);
        Tariff tariff = tariffService.getTariffByCode(request.getTariffCode());

        offering.setName(request.getName());
        offering.setDescription(request.getDescription());
        offering.setTariff(tariff);
        offering.setStatus(request.getStatus());
        offering.setEffectiveFrom(request.getEffectiveFrom());
        offering.setEffectiveTo(request.getEffectiveTo());

        offering = productOfferingRepository.save(offering);
        ProductOfferingResponse response = productOfferingMapper.toResponse(offering);
        outboxEventService.publish(AGGREGATE_TYPE, offering.getId(), "ProductOfferingUpdated", response);
        return response;
    }

    @Transactional
    public ProductOfferingResponse patchProductOffering(String code, ProductOfferingPatchRequest request) {
        ProductOffering offering = getProductOfferingByCode(code);

        if (request.getName() != null) offering.setName(request.getName());
        if (request.getDescription() != null) offering.setDescription(request.getDescription());
        if (request.getStatus() != null) offering.setStatus(request.getStatus());
        if (request.getEffectiveFrom() != null) offering.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) offering.setEffectiveTo(request.getEffectiveTo());
        if (request.getTariffCode() != null) {
            Tariff tariff = tariffService.getTariffByCode(request.getTariffCode());
            offering.setTariff(tariff);
        }

        offering = productOfferingRepository.save(offering);
        ProductOfferingResponse response = productOfferingMapper.toResponse(offering);
        outboxEventService.publish(AGGREGATE_TYPE, offering.getId(), "ProductOfferingUpdated", response);
        return response;
    }

    @Transactional
    public void deleteProductOffering(String code) {
        ProductOffering offering = getProductOfferingByCode(code);
        offering.setStatus("INACTIVE");
        offering = productOfferingRepository.save(offering);
        outboxEventService.publish(AGGREGATE_TYPE, offering.getId(), "ProductOfferingDeactivated",
                productOfferingMapper.toResponse(offering));
    }
}
