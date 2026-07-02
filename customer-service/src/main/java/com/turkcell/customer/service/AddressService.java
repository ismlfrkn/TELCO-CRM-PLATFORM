package com.turkcell.customer.service;

import com.turkcell.customer.dto.request.AddressCreateRequest;
import com.turkcell.customer.dto.response.AddressResponse;
import com.turkcell.customer.entity.Address;
import com.turkcell.customer.entity.Customer;
import com.turkcell.customer.mapper.AddressMapper;
import com.turkcell.customer.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CustomerService customerService;
    private final AddressMapper addressMapper;

    public AddressService(AddressRepository addressRepository, CustomerService customerService, AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.customerService = customerService;
        this.addressMapper = addressMapper;
    }

    @Transactional
    public AddressResponse addAddress(UUID customerId, AddressCreateRequest request) {
        Customer customer = customerService.getCustomerById(customerId);

        Address address = new Address();
        address.setCustomer(customer);
        address.setLine1(request.getLine1());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setPostalCode(request.getPostalCode());
        address.setDefault(request.isDefault());

        return addressMapper.toResponse(addressRepository.save(address));
    }

    public List<AddressResponse> getAddresses(UUID customerId) {
        customerService.getCustomerById(customerId);
        return addressRepository.findByCustomerId(customerId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }
}
