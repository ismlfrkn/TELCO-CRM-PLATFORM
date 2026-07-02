package com.turkcell.productcatalog.service;

import com.turkcell.productcatalog.dto.request.AddonCreateRequest;
import com.turkcell.productcatalog.dto.request.AddonPatchRequest;
import com.turkcell.productcatalog.dto.request.AddonUpdateRequest;
import com.turkcell.productcatalog.dto.response.AddonResponse;
import com.turkcell.productcatalog.entity.Addon;
import com.turkcell.productcatalog.exception.AddonNotFoundException;
import com.turkcell.productcatalog.mapper.AddonMapper;
import com.turkcell.productcatalog.exception.AddonNotFoundException;
import com.turkcell.productcatalog.repository.AddonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddonService {

    private final AddonRepository addonRepository;
    private final AddonMapper addonMapper;

    public AddonService(AddonRepository addonRepository, AddonMapper addonMapper) {
        this.addonRepository = addonRepository;
        this.addonMapper = addonMapper;
    }

    public Page<AddonResponse> getAllAddons(String tariffCode, Pageable pageable) {
        if (tariffCode != null && !tariffCode.isBlank()) {
            return addonRepository.findAllByTariffCodeAndActive(tariffCode, pageable)
                    .map(addonMapper::toResponse);
        }
        return addonRepository.findAllByStatusNot("INACTIVE", pageable)
                .map(addonMapper::toResponse);
    }

    public AddonResponse getAddonResponseByCode(String code) {
        return addonMapper.toResponse(getAddonByCode(code));
    }

    public Addon getAddonByCode(String code) {
        return addonRepository.findByCodeAndStatusNot(code, "INACTIVE")
                .orElseThrow(() -> new AddonNotFoundException("Addon not found with code: " + code));
    }

    @Transactional
    public AddonResponse createAddon(AddonCreateRequest request) {
        Addon addon = new Addon();
        addon.setCode(request.getCode());
        addon.setName(request.getName());
        addon.setPrice(request.getPrice());
        addon.setType(request.getType());
        addon.setValidityDays(request.getValidityDays());
        addon.setCurrency(request.getCurrency());
        addon.setStatus(request.getStatus());
        
        return addonMapper.toResponse(addonRepository.save(addon));
    }

    @Transactional
    public AddonResponse updateAddon(String code, AddonUpdateRequest request) {
        Addon addon = getAddonByCode(code);
        
        addon.setName(request.getName());
        addon.setPrice(request.getPrice());
        addon.setType(request.getType());
        addon.setValidityDays(request.getValidityDays());
        addon.setCurrency(request.getCurrency());
        addon.setStatus(request.getStatus());
        
        return addonMapper.toResponse(addonRepository.save(addon));
    }

    @Transactional
    public AddonResponse patchAddon(String code, AddonPatchRequest request) {
        Addon addon = getAddonByCode(code);
        
        if (request.getName() != null) addon.setName(request.getName());
        if (request.getPrice() != null) addon.setPrice(request.getPrice());
        if (request.getType() != null) addon.setType(request.getType());
        if (request.getValidityDays() != null) addon.setValidityDays(request.getValidityDays());
        if (request.getCurrency() != null) addon.setCurrency(request.getCurrency());
        if (request.getStatus() != null) addon.setStatus(request.getStatus());
        
        return addonMapper.toResponse(addonRepository.save(addon));
    }

    @Transactional
    public void deleteAddon(String code) {
        Addon addon = getAddonByCode(code);
        addon.setStatus("INACTIVE");
        addonRepository.save(addon);
    }
}
