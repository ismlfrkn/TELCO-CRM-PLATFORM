package com.turkcell.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.payment.config.GatewayTrustProperties;
import com.turkcell.payment.dto.request.PaymentCreateRequest;
import com.turkcell.payment.dto.response.PaymentResponse;
import com.turkcell.payment.exception.PaymentNotFoundException;
import com.turkcell.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private GatewayTrustProperties gatewayTrustProperties;

    private PaymentCreateRequest validRequest() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setAmount(new BigDecimal("49.90"));
        request.setMethod("CARD");
        return request;
    }

    @Test
    void createPayment_withValidRequestAndIdempotencyKey_returns201() throws Exception {
        PaymentResponse response = new PaymentResponse();
        response.setId(UUID.randomUUID());
        response.setAmount(new BigDecimal("49.90"));
        response.setMethod("CARD");
        response.setStatus("COMPLETED");
        response.setCreatedAt(Instant.now());

        when(paymentService.createPayment(eq("idem-key-1"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void createPayment_withoutIdempotencyKeyHeader_returns400MissingHeader() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://telco.example/errors/missing-header"))
                .andExpect(jsonPath("$.title").value("Missing required header"));
    }

    @Test
    void createPayment_withInvalidAmount_returns400WithValidationErrors() throws Exception {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setAmount(new BigDecimal("0.00"));
        request.setMethod("CARD");

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://telco.example/errors/validation-failed"))
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void createPayment_withBlankMethod_returns400WithValidationErrors() throws Exception {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setAmount(new BigDecimal("49.90"));
        request.setMethod("");

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-key-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.method").exists());
    }

    @Test
    void getById_whenPaymentNotFound_returns404ProblemDetail() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(paymentService.getPaymentResponseById(missingId))
                .thenThrow(new PaymentNotFoundException("Payment not found with id: " + missingId));

        mockMvc.perform(get("/api/v1/payments/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://telco.example/errors/payment-not-found"))
                .andExpect(jsonPath("$.title").value("Payment not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createPayment_withMalformedJson_returns400MalformedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-key-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://telco.example/errors/malformed-request"))
                .andExpect(jsonPath("$.instance").value("/api/v1/payments"));
    }
}
