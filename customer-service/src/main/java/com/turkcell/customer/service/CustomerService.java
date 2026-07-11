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
import com.turkcell.customer.validation.IdentityNumberValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private static final String AGGREGATE_TYPE = "Customer";

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final OutboxEventService outboxEventService;
    private final AuditLogService auditLogService;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper,
                            OutboxEventService outboxEventService, AuditLogService auditLogService) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.outboxEventService = outboxEventService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        if (!IdentityNumberValidator.isValid(request.getType(), request.getIdentityNumber())) {
            throw new InvalidIdentityNumberException(
                    "Identity number is not a valid " + request.getType() + " identity number");
        }
        if (customerRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            throw new DuplicateIdentityNumberException(
                    "Customer already exists with identity number: " + request.getIdentityNumber());
        }

        Customer customer = new Customer();
        customer.setType(request.getType());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setIdentityNumber(request.getIdentityNumber());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setStatus("PENDING");

        customer = customerRepository.save(customer);
        CustomerResponse response = customerMapper.toResponse(customer);
        outboxEventService.publish(AGGREGATE_TYPE, customer.getId(), "CustomerCreated", response);
        auditLogService.record("CUSTOMER_CREATED", AGGREGATE_TYPE, customer.getId(), null, response);
        return response;
    }

    public CustomerResponse getCustomerResponseById(UUID id) {
        return customerMapper.toResponse(getCustomerById(id));
    }

    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        return customerRepository.findAllByDeletedFalse(pageable).map(customerMapper::toResponse);
    }

    public Customer getCustomerById(UUID id) {
        return customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerUpdateRequest request) {
        Customer customer = getCustomerById(id);

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        customer = customerRepository.save(customer);
        CustomerResponse response = customerMapper.toResponse(customer);
        outboxEventService.publish(AGGREGATE_TYPE, customer.getId(), "CustomerUpdated", response);
        auditLogService.record("CUSTOMER_UPDATED", AGGREGATE_TYPE, customer.getId(), null, response);
        return response;
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = getCustomerById(id);
        customer.setDeleted(true);
        customer = customerRepository.save(customer);
        CustomerResponse response = customerMapper.toResponse(customer);
        outboxEventService.publish(AGGREGATE_TYPE, customer.getId(), "CustomerDeleted", response);
        auditLogService.record("CUSTOMER_DELETED", AGGREGATE_TYPE, customer.getId(), null, response);
    }

    @Transactional
    public CustomerResponse approveKyc(UUID id) {
        Customer customer = getCustomerById(id);
        requirePendingStatus(customer);
        customer.setStatus("ACTIVE");
        customer = customerRepository.save(customer);
        CustomerResponse response = customerMapper.toResponse(customer);
        outboxEventService.publish(AGGREGATE_TYPE, customer.getId(), "CustomerKycApproved", response);
        auditLogService.record("CUSTOMER_KYC_APPROVED", AGGREGATE_TYPE, customer.getId(), null, response);
        return response;
    }

    @Transactional
    public CustomerResponse rejectKyc(UUID id) {
        Customer customer = getCustomerById(id);
        requirePendingStatus(customer);
        customer.setStatus("REJECTED");
        customer = customerRepository.save(customer);
        CustomerResponse response = customerMapper.toResponse(customer);
        outboxEventService.publish(AGGREGATE_TYPE, customer.getId(), "CustomerKycRejected", response);
        auditLogService.record("CUSTOMER_KYC_REJECTED", AGGREGATE_TYPE, customer.getId(), null, response);
        return response;
    }

    private void requirePendingStatus(Customer customer) {
        if (!"PENDING".equals(customer.getStatus())) {
            throw new InvalidKycTransitionException(
                    "KYC decision requires status PENDING, current status: " + customer.getStatus());
        }
    }
}
