package com.turkcell.productcatalog.controller;

import com.turkcell.productcatalog.dto.request.TariffCreateRequest;
import com.turkcell.productcatalog.dto.request.TariffPatchRequest;
import com.turkcell.productcatalog.dto.request.TariffUpdateRequest;
import com.turkcell.productcatalog.dto.response.TariffResponse;
import com.turkcell.productcatalog.dto.response.TariffVersionResponse;
import com.turkcell.productcatalog.service.TariffService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tariffs")
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @GetMapping
    public Page<TariffResponse> getAll(Pageable pageable) {
        return tariffService.getAllTariffs(pageable);
    }

    @GetMapping("/{code}")
    public TariffResponse getByCode(@PathVariable String code) {
        return tariffService.getTariffResponseByCode(code);
    }

    @GetMapping("/{code}/versions")
    public List<TariffVersionResponse> getVersionHistory(@PathVariable String code) {
        return tariffService.getTariffVersionHistory(code);
    }

    @GetMapping("/{code}/versions/{version}")
    public TariffVersionResponse getVersion(@PathVariable String code, @PathVariable int version) {
        return tariffService.getTariffVersion(code, version);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TariffResponse create(@Valid @RequestBody TariffCreateRequest request) {
        return tariffService.createTariff(request);
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public TariffResponse update(@PathVariable String code, @Valid @RequestBody TariffUpdateRequest request) {
        return tariffService.updateTariff(code, request);
    }

    @PatchMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public TariffResponse patch(@PathVariable String code, @RequestBody TariffPatchRequest request) {
        return tariffService.patchTariff(code, request);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable String code) {
        tariffService.deleteTariff(code);
    }

    @PostMapping("/{code}/addons/{addonCode}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public void linkAddon(@PathVariable String code, @PathVariable String addonCode) {
        tariffService.linkAddonToTariff(code, addonCode);
    }
}
