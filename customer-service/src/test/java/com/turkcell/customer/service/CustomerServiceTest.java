package com.turkcell.customer.service;

import com.turkcell.customer.dto.request.CustomerCreateRequest;
import com.turkcell.customer.dto.request.CustomerUpdateRequest;
import com.turkcell.customer.dto.response.CustomerResponse;
import com.turkcell.customer.entity.Customer;
import com.turkcell.customer.exception.CustomerNotFoundException;
import com.turkcell.customer.exception.DuplicateIdentityNumberException;
import com.turkcell.customer.exception.InvalidIdentityNumberException;
import com.turkcell.customer.exception.InvalidKycTransitionException;
import com.turkcell.customer.mapper.CustomerMapper;
import com.turkcell.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private static final String VALID_TCKN = "10000000146";

    private CustomerRepository customerRepository;
    private OutboxEventService outboxEventService;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        outboxEventService = mock(OutboxEventService.class);
        CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);
        customerService = new CustomerService(customerRepository, customerMapper, outboxEventService);

        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createCustomer_withValidIndividualTckn_createsPendingCustomer() {
        when(customerRepository.existsByIdentityNumber(VALID_TCKN)).thenReturn(false);

        CustomerCreateRequest request = individualRequest(VALID_TCKN);

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getIdentityNumber()).isEqualTo(VALID_TCKN);
        verify(outboxEventService).publish(eq("Customer"), any(), eq("CustomerCreated"), any());
    }

    @Test
    void createCustomer_withInvalidTckn_throwsInvalidIdentityNumberException() {
        CustomerCreateRequest request = individualRequest("12345678900");

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(InvalidIdentityNumberException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createCustomer_withAlreadyRegisteredIdentityNumber_throwsDuplicateIdentityNumberException() {
        when(customerRepository.existsByIdentityNumber(VALID_TCKN)).thenReturn(true);

        CustomerCreateRequest request = individualRequest(VALID_TCKN);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateIdentityNumberException.class);
    }

    @Test
    void createCustomer_withInvalidCorporateVkn_throwsInvalidIdentityNumberException() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setType("CORPORATE");
        request.setFirstName("Acme");
        request.setLastName("Corp");
        request.setIdentityNumber("0234567890"); // leading zero, gecersiz VKN

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(InvalidIdentityNumberException.class);
    }

    @Test
    void approveKyc_fromPending_activatesCustomer() {
        Customer customer = pendingCustomer();
        when(customerRepository.findByIdAndDeletedFalse(customer.getId())).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.approveKyc(customer.getId());

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(outboxEventService).publish(eq("Customer"), eq(customer.getId()), eq("CustomerKycApproved"), any());
    }

    @Test
    void approveKyc_whenNotPending_throwsInvalidKycTransitionException() {
        Customer customer = pendingCustomer();
        customer.setStatus("ACTIVE");
        when(customerRepository.findByIdAndDeletedFalse(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.approveKyc(customer.getId()))
                .isInstanceOf(InvalidKycTransitionException.class);
    }

    @Test
    void rejectKyc_fromPending_setsRejected() {
        Customer customer = pendingCustomer();
        when(customerRepository.findByIdAndDeletedFalse(customer.getId())).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.rejectKyc(customer.getId());

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        verify(outboxEventService).publish(eq("Customer"), eq(customer.getId()), eq("CustomerKycRejected"), any());
    }

    @Test
    void rejectKyc_whenAlreadyRejected_throwsInvalidKycTransitionException() {
        Customer customer = pendingCustomer();
        customer.setStatus("REJECTED");
        when(customerRepository.findByIdAndDeletedFalse(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.rejectKyc(customer.getId()))
                .isInstanceOf(InvalidKycTransitionException.class);
    }

    @Test
    void updateCustomer_changesEditableFieldsOnly() {
        Customer customer = pendingCustomer();
        when(customerRepository.findByIdAndDeletedFalse(customer.getId())).thenReturn(Optional.of(customer));

        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setEmail("updated@example.com");
        request.setPhone("5559998877");

        CustomerResponse response = customerService.updateCustomer(customer.getId(), request);

        assertThat(response.getFirstName()).isEqualTo("Updated");
        assertThat(response.getIdentityNumber()).isEqualTo(VALID_TCKN); // degistirilemez
        verify(outboxEventService).publish(eq("Customer"), eq(customer.getId()), eq("CustomerUpdated"), any());
    }

    @Test
    void deleteCustomer_softDeletesByMarkingDeletedFlag() {
        Customer customer = pendingCustomer();
        when(customerRepository.findByIdAndDeletedFalse(customer.getId())).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(customer.getId());

        assertThat(customer.isDeleted()).isTrue();
        verify(outboxEventService).publish(eq("Customer"), eq(customer.getId()), eq("CustomerDeleted"), any());
    }

    @Test
    void getCustomerById_whenMissingOrDeleted_throwsCustomerNotFoundException() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(id))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    private CustomerCreateRequest individualRequest(String identityNumber) {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setType("INDIVIDUAL");
        request.setFirstName("Serhat");
        request.setLastName("Ozdemir");
        request.setIdentityNumber(identityNumber);
        request.setEmail("serhat@example.com");
        request.setPhone("5551234567");
        return request;
    }

    private Customer pendingCustomer() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setType("INDIVIDUAL");
        customer.setFirstName("Serhat");
        customer.setLastName("Ozdemir");
        customer.setIdentityNumber(VALID_TCKN);
        customer.setEmail("serhat@example.com");
        customer.setPhone("5551234567");
        customer.setStatus("PENDING");
        return customer;
    }
}
