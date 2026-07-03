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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
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

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    public CustomerResponse getCustomerResponseById(UUID id) {
        return customerMapper.toResponse(getCustomerById(id));
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

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = getCustomerById(id);
        customer.setDeleted(true);
        customerRepository.save(customer);
    }

    @Transactional
    public CustomerResponse approveKyc(UUID id) {
        Customer customer = getCustomerById(id);
        requirePendingStatus(customer);
        customer.setStatus("ACTIVE");
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse rejectKyc(UUID id) {
        Customer customer = getCustomerById(id);
        requirePendingStatus(customer);
        customer.setStatus("REJECTED");
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    private void requirePendingStatus(Customer customer) {
        if (!"PENDING".equals(customer.getStatus())) {
            throw new InvalidKycTransitionException(
                    "KYC decision requires status PENDING, current status: " + customer.getStatus());
        }
    }
}
