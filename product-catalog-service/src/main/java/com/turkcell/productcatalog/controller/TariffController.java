package com.turkcell.productcatalog.controller;

import com.turkcell.productcatalog.dto.request.TariffCreateRequest;
import com.turkcell.productcatalog.dto.request.TariffPatchRequest;
import com.turkcell.productcatalog.dto.request.TariffUpdateRequest;
import com.turkcell.productcatalog.dto.response.TariffResponse;
import com.turkcell.productcatalog.service.TariffService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffResponse create(@Valid @RequestBody TariffCreateRequest request) {
        return tariffService.createTariff(request);
    }

    @PutMapping("/{code}")
    public TariffResponse update(@PathVariable String code, @Valid @RequestBody TariffUpdateRequest request) {
        return tariffService.updateTariff(code, request);
    }

    @PatchMapping("/{code}")
    public TariffResponse patch(@PathVariable String code, @RequestBody TariffPatchRequest request) {
        return tariffService.patchTariff(code, request);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        tariffService.deleteTariff(code);
    }

    @PostMapping("/{code}/addons/{addonCode}")
    @ResponseStatus(HttpStatus.OK)
    public void linkAddon(@PathVariable String code, @PathVariable String addonCode) {
        tariffService.linkAddonToTariff(code, addonCode);
    }
}
