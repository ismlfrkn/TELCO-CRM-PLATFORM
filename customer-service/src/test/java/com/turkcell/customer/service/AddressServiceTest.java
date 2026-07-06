package com.turkcell.customer.service;

import com.turkcell.customer.dto.request.AddressCreateRequest;
import com.turkcell.customer.dto.response.AddressResponse;
import com.turkcell.customer.entity.Address;
import com.turkcell.customer.entity.Customer;
import com.turkcell.customer.exception.CustomerNotFoundException;
import com.turkcell.customer.mapper.AddressMapper;
import com.turkcell.customer.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddressServiceTest {

    private AddressRepository addressRepository;
    private CustomerService customerService;
    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressRepository = mock(AddressRepository.class);
        customerService = mock(CustomerService.class);
        AddressMapper addressMapper = Mappers.getMapper(AddressMapper.class);
        addressService = new AddressService(addressRepository, customerService, addressMapper);

        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addAddress_whenCustomerExists_savesAddressLinkedToCustomer() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        when(customerService.getCustomerById(customer.getId())).thenReturn(customer);

        AddressCreateRequest request = new AddressCreateRequest();
        request.setLine1("Ataturk Cad. No:1");
        request.setCity("Istanbul");
        request.setDistrict("Kadikoy");
        request.setPostalCode("34000");
        request.setDefault(true);

        AddressResponse response = addressService.addAddress(customer.getId(), request);

        assertThat(response.getCity()).isEqualTo("Istanbul");
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void addAddress_whenCustomerMissing_throwsCustomerNotFoundException() {
        UUID customerId = UUID.randomUUID();
        when(customerService.getCustomerById(customerId))
                .thenThrow(new CustomerNotFoundException("Customer not found with id: " + customerId));

        AddressCreateRequest request = new AddressCreateRequest();
        request.setLine1("Ataturk Cad. No:1");
        request.setCity("Istanbul");

        assertThatThrownBy(() -> addressService.addAddress(customerId, request))
                .isInstanceOf(CustomerNotFoundException.class);
        verify(addressRepository, never()).save(any());
    }

    @Test
    void getAddresses_returnsMappedResponsesForCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerService.getCustomerById(customerId)).thenReturn(customer);

        Address address = new Address();
        address.setId(UUID.randomUUID());
        address.setCustomer(customer);
        address.setLine1("Ataturk Cad. No:1");
        address.setCity("Istanbul");
        when(addressRepository.findByCustomerId(customerId)).thenReturn(List.of(address));

        List<AddressResponse> responses = addressService.getAddresses(customerId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCity()).isEqualTo("Istanbul");
    }

    @Test
    void getAddresses_whenCustomerMissing_throwsCustomerNotFoundException() {
        UUID customerId = UUID.randomUUID();
        when(customerService.getCustomerById(customerId))
                .thenThrow(new CustomerNotFoundException("Customer not found with id: " + customerId));

        assertThatThrownBy(() -> addressService.getAddresses(customerId))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}
