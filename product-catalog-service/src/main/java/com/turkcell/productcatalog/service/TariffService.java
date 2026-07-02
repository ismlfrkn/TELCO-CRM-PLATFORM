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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TariffService {

    private final TariffRepository tariffRepository;
    private final AddonService addonService;
    private final TariffMapper tariffMapper;

    public TariffService(TariffRepository tariffRepository, AddonService addonService, TariffMapper tariffMapper) {
        this.tariffRepository = tariffRepository;
        this.addonService = addonService;
        this.tariffMapper = tariffMapper;
    }

    public Page<TariffResponse> getAllTariffs(Pageable pageable) {
        return tariffRepository.findAllByStatusNot("INACTIVE", pageable)
                .map(tariffMapper::toResponse);
    }

    public TariffResponse getTariffResponseByCode(String code) {
        return tariffMapper.toResponse(getTariffByCode(code));
    }

    public Tariff getTariffByCode(String code) {
        return tariffRepository.findByCodeAndStatusNot(code, "INACTIVE")
                .orElseThrow(() -> new TariffNotFoundException("Tariff not found with code: " + code));
    }

    @Transactional
    public TariffResponse createTariff(TariffCreateRequest request) {
        Tariff tariff = new Tariff();
        tariff.setCode(request.getCode());
        tariff.setName(request.getName());
        tariff.setType(request.getType());
        tariff.setMonthlyFee(request.getMonthlyFee());
        tariff.setMinutesIncluded(request.getMinutesIncluded());
        tariff.setSmsIncluded(request.getSmsIncluded());
        tariff.setDataMbIncluded(request.getDataMbIncluded());
        tariff.setStatus(request.getStatus());
        tariff.setCurrency(request.getCurrency());
        tariff.setEffectiveFrom(request.getEffectiveFrom());
        tariff.setEffectiveTo(request.getEffectiveTo());
        
        return tariffMapper.toResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public TariffResponse updateTariff(String code, TariffUpdateRequest request) {
        Tariff tariff = getTariffByCode(code);
        
        tariff.setName(request.getName());
        tariff.setType(request.getType());
        tariff.setMonthlyFee(request.getMonthlyFee());
        tariff.setMinutesIncluded(request.getMinutesIncluded());
        tariff.setSmsIncluded(request.getSmsIncluded());
        tariff.setDataMbIncluded(request.getDataMbIncluded());
        tariff.setStatus(request.getStatus());
        tariff.setCurrency(request.getCurrency());
        tariff.setEffectiveFrom(request.getEffectiveFrom());
        tariff.setEffectiveTo(request.getEffectiveTo());
        
        return tariffMapper.toResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public TariffResponse patchTariff(String code, TariffPatchRequest request) {
        Tariff tariff = getTariffByCode(code);
        
        if (request.getName() != null) tariff.setName(request.getName());
        if (request.getType() != null) tariff.setType(request.getType());
        if (request.getMonthlyFee() != null) tariff.setMonthlyFee(request.getMonthlyFee());
        if (request.getMinutesIncluded() != null) tariff.setMinutesIncluded(request.getMinutesIncluded());
        if (request.getSmsIncluded() != null) tariff.setSmsIncluded(request.getSmsIncluded());
        if (request.getDataMbIncluded() != null) tariff.setDataMbIncluded(request.getDataMbIncluded());
        if (request.getStatus() != null) tariff.setStatus(request.getStatus());
        if (request.getCurrency() != null) tariff.setCurrency(request.getCurrency());
        if (request.getEffectiveFrom() != null) tariff.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) tariff.setEffectiveTo(request.getEffectiveTo());
        
        return tariffMapper.toResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public void deleteTariff(String code) {
        Tariff tariff = getTariffByCode(code);
        tariff.setStatus("INACTIVE");
        tariffRepository.save(tariff);
    }

    @Transactional
    public void linkAddonToTariff(String tariffCode, String addonCode) {
        Tariff tariff = getTariffByCode(tariffCode);
        Addon addon = addonService.getAddonByCode(addonCode);
        
        tariff.getAddons().add(addon);
        tariffRepository.save(tariff);
    }
}
