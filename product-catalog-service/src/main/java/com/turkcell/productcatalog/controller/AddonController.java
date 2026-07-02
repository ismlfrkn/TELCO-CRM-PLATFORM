package com.turkcell.productcatalog.controller;

import com.turkcell.productcatalog.dto.request.AddonCreateRequest;
import com.turkcell.productcatalog.dto.request.AddonPatchRequest;
import com.turkcell.productcatalog.dto.request.AddonUpdateRequest;
import com.turkcell.productcatalog.dto.response.AddonResponse;
import com.turkcell.productcatalog.service.AddonService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/addons")
public class AddonController {

    private final AddonService addonService;

    public AddonController(AddonService addonService) {
        this.addonService = addonService;
    }

    @GetMapping
    public Page<AddonResponse> getAll(@RequestParam(required = false) String tariffCode, Pageable pageable) {
        return addonService.getAllAddons(tariffCode, pageable);
    }

    @GetMapping("/{code}")
    public AddonResponse getByCode(@PathVariable String code) {
        return addonService.getAddonResponseByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddonResponse create(@Valid @RequestBody AddonCreateRequest request) {
        return addonService.createAddon(request);
    }

    @PutMapping("/{code}")
    public AddonResponse update(@PathVariable String code, @Valid @RequestBody AddonUpdateRequest request) {
        return addonService.updateAddon(code, request);
    }

    @PatchMapping("/{code}")
    public AddonResponse patch(@PathVariable String code, @RequestBody AddonPatchRequest request) {
        return addonService.patchAddon(code, request);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        addonService.deleteAddon(code);
    }
}
