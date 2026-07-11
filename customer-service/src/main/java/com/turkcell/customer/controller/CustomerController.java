package com.turkcell.customer.controller;

import com.turkcell.customer.dto.request.AddressCreateRequest;
import com.turkcell.customer.dto.request.CustomerCreateRequest;
import com.turkcell.customer.dto.request.CustomerUpdateRequest;
import com.turkcell.customer.dto.request.DocumentCreateRequest;
import com.turkcell.customer.dto.response.AddressResponse;
import com.turkcell.customer.dto.response.CustomerResponse;
import com.turkcell.customer.dto.response.DocumentResponse;
import com.turkcell.customer.service.AddressService;
import com.turkcell.customer.service.CustomerService;
import com.turkcell.customer.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final AddressService addressService;
    private final DocumentService documentService;

    public CustomerController(CustomerService customerService, AddressService addressService, DocumentService documentService) {
        this.customerService = customerService;
        this.addressService = addressService;
        this.documentService = documentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerCreateRequest request) {
        return customerService.createCustomer(request);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable UUID id) {
        return customerService.getCustomerResponseById(id);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
    }

    @PostMapping("/{id}/kyc/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse approveKyc(@PathVariable UUID id) {
        return customerService.approveKyc(id);
    }

    @PostMapping("/{id}/kyc/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse rejectKyc(@PathVariable UUID id) {
        return customerService.rejectKyc(id);
    }

    @PostMapping("/{id}/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@PathVariable UUID id, @Valid @RequestBody AddressCreateRequest request) {
        return addressService.addAddress(id, request);
    }

    @GetMapping("/{id}/addresses")
    public List<AddressResponse> getAddresses(@PathVariable UUID id) {
        return addressService.getAddresses(id);
    }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse addDocument(@PathVariable UUID id, @Valid @RequestBody DocumentCreateRequest request) {
        return documentService.addDocument(id, request);
    }

    @GetMapping("/{id}/documents")
    public List<DocumentResponse> getDocuments(@PathVariable UUID id) {
        return documentService.getDocuments(id);
    }
}
