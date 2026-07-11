package com.turkcell.order.exception;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFoundException(OrderNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/order-not-found"));
        problemDetail.setTitle("Order not found");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFoundException(CustomerNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/customer-not-found"));
        problemDetail.setTitle("Customer not found");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFoundException(ProductNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/product-not-found"));
        problemDetail.setTitle("Product not found");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ProblemDetail handleIdempotencyKeyReusedException(IdempotencyKeyReusedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/idempotency-key-reused"));
        problemDetail.setTitle("Idempotency-Key reused with a different request");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(OrderProcessingInProgressException.class)
    public ProblemDetail handleOrderProcessingInProgressException(OrderProcessingInProgressException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/order-processing-in-progress"));
        problemDetail.setTitle("Order creation already in progress");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ProblemDetail handleInvalidOrderStateException(InvalidOrderStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/invalid-order-state"));
        problemDetail.setTitle("Invalid order state");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ProblemDetail handleCallNotPermittedException(CallNotPermittedException ex) {
        log.error("Circuit breaker '{}' is OPEN, rejecting call without hitting the downstream service", ex.getCausingCircuitBreakerName());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "A downstream service is temporarily unavailable, please retry later");
        problemDetail.setType(URI.create("https://telco.example/errors/downstream-service-unavailable"));
        problemDetail.setTitle("Downstream service unavailable");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(FeignException.class)
    public ProblemDetail handleFeignException(FeignException ex) {
        log.error("Downstream service call failed", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "A downstream service call failed unexpectedly");
        problemDetail.setType(URI.create("https://telco.example/errors/downstream-service-error"));
        problemDetail.setTitle("Downstream service error");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/missing-header"));
        problemDetail.setTitle("Missing required header");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
        problem.setType(URI.create("https://telco.example/errors/malformed-request"));
        problem.setTitle("Malformed request body");
        problem.setInstance(URI.create(request.getRequestURI()));
        addCommonProperties(problem);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        problemDetail.setType(URI.create("https://telco.example/errors/validation-failed"));
        problemDetail.setTitle("Validation Failed");

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problemDetail.setProperty("errors", errors);
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setType(URI.create("https://telco.example/errors/internal-server-error"));
        problemDetail.setTitle("Internal Server Error");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    private void addCommonProperties(ProblemDetail problemDetail) {
        problemDetail.setProperty("timestamp", Instant.now());
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            problemDetail.setProperty("correlationId", correlationId);
        }
    }
}
