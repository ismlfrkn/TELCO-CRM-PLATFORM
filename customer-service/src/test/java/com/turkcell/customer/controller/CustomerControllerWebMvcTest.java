package com.turkcell.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.customer.config.GatewayTrustProperties;
import com.turkcell.customer.dto.request.CustomerCreateRequest;
import com.turkcell.customer.dto.response.CustomerResponse;
import com.turkcell.customer.exception.CustomerNotFoundException;
import com.turkcell.customer.exception.DuplicateIdentityNumberException;
import com.turkcell.customer.service.AddressService;
import com.turkcell.customer.service.CustomerService;
import com.turkcell.customer.service.DocumentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bolum 11'deki RFC 7807 hata formati, HTTP status kodlari ve validation hata govdesi
 * bugune kadar hic HTTP seviyesinde test edilmemisti (denetim raporu bulgusu #3) - bu
 * testler controller + GlobalExceptionHandler ciftini gercek Spring MVC dispatch'i
 * uzerinden dogruluyor. Security filter zinciri (addFilters = false) kasitli olarak
 * disarida birakildi: amac auth degil, hata sozlesmesinin kendisi.
 */
@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private AddressService addressService;

    @MockBean
    private DocumentService documentService;

    // GatewayHeaderAuthenticationFilter bean'i @WebMvcTest'in Filter-tipi tarama kapsamina giriyor
    // (addFilters=false sadece MockMvc zincirinden cikarir, context'ten degil) ve bu bean'e ihtiyac
    // duyuyor - gercekte hic calismiyor, sadece context refresh olabilsin diye mock'lanmasi gerekiyor.
    @MockBean
    private GatewayTrustProperties gatewayTrustProperties;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void createCustomer_withValidRequest_returns201WithBody() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setType("INDIVIDUAL");
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setIdentityNumber("10000000146");

        CustomerResponse response = new CustomerResponse();
        response.setId(UUID.randomUUID());
        response.setType("INDIVIDUAL");
        response.setFirstName("Ada");
        response.setLastName("Lovelace");
        response.setIdentityNumber("10000000146");
        response.setStatus("PENDING");
        response.setCreatedAt(Instant.now());

        when(customerService.createCustomer(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.firstName").value("Ada"));
    }

    @Test
    void createCustomer_withMissingRequiredFields_returns400WithValidationErrors() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setEmail("not-an-email");
        // type, firstName, lastName, identityNumber left blank on purpose

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://telco.example/errors/validation-failed"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.identityNumber").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void getAll_returnsPaginatedCustomerList() throws Exception {
        CustomerResponse response = new CustomerResponse();
        response.setId(UUID.randomUUID());
        response.setFirstName("Ada");
        response.setLastName("Lovelace");
        response.setIdentityNumber("10000000146");
        response.setStatus("ACTIVE");

        when(customerService.getAllCustomers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Ada"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_whenCustomerNotFound_returns404ProblemDetail() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(customerService.getCustomerResponseById(missingId))
                .thenThrow(new CustomerNotFoundException("Customer not found with id: " + missingId));

        mockMvc.perform(get("/api/v1/customers/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://telco.example/errors/customer-not-found"))
                .andExpect(jsonPath("$.title").value("Customer not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Customer not found with id: " + missingId));
    }

    @Test
    void createCustomer_whenIdentityNumberAlreadyRegistered_returns409ProblemDetail() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setType("INDIVIDUAL");
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setIdentityNumber("10000000146");

        when(customerService.createCustomer(any()))
                .thenThrow(new DuplicateIdentityNumberException("Customer already exists with identity number: 10000000146"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://telco.example/errors/duplicate-identity-number"))
                .andExpect(jsonPath("$.title").value("Customer already exists"));
    }

    @Test
    void createCustomer_withMalformedJson_returns400MalformedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://telco.example/errors/malformed-request"))
                .andExpect(jsonPath("$.title").value("Malformed request body"))
                .andExpect(jsonPath("$.instance").value("/api/v1/customers"));
    }

    @Test
    void getById_whenCorrelationIdPresentInMdc_isEchoedInProblemDetail() throws Exception {
        MDC.put("correlationId", "test-correlation-id");
        UUID missingId = UUID.randomUUID();
        when(customerService.getCustomerResponseById(missingId))
                .thenThrow(new CustomerNotFoundException("Customer not found with id: " + missingId));

        mockMvc.perform(get("/api/v1/customers/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.correlationId").value("test-correlation-id"));
    }
}
