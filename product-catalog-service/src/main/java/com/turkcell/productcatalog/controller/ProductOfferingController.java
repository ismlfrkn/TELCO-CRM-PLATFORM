package com.turkcell.productcatalog.controller;

import com.turkcell.productcatalog.dto.request.ProductOfferingCreateRequest;
import com.turkcell.productcatalog.dto.request.ProductOfferingPatchRequest;
import com.turkcell.productcatalog.dto.request.ProductOfferingUpdateRequest;
import com.turkcell.productcatalog.dto.response.ProductOfferingResponse;
import com.turkcell.productcatalog.service.ProductOfferingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/product-offerings")
public class ProductOfferingController {

    private final ProductOfferingService productOfferingService;

    public ProductOfferingController(ProductOfferingService productOfferingService) {
        this.productOfferingService = productOfferingService;
    }

    @GetMapping
    public Page<ProductOfferingResponse> getAll(Pageable pageable) {
        return productOfferingService.getAllProductOfferings(pageable);
    }

    @GetMapping("/{code}")
    public ProductOfferingResponse getByCode(@PathVariable String code) {
        return productOfferingService.getProductOfferingResponseByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductOfferingResponse create(@Valid @RequestBody ProductOfferingCreateRequest request) {
        return productOfferingService.createProductOffering(request);
    }

    @PutMapping("/{code}")
    public ProductOfferingResponse update(@PathVariable String code, @Valid @RequestBody ProductOfferingUpdateRequest request) {
        return productOfferingService.updateProductOffering(code, request);
    }

    @PatchMapping("/{code}")
    public ProductOfferingResponse patch(@PathVariable String code, @RequestBody ProductOfferingPatchRequest request) {
        return productOfferingService.patchProductOffering(code, request);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        productOfferingService.deleteProductOffering(code);
    }
}
