package com.turkcell.payment.exception;

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

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFoundException(PaymentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/payment-not-found"));
        problemDetail.setTitle("Payment not found");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ProblemDetail handleWalletNotFoundException(WalletNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/wallet-not-found"));
        problemDetail.setTitle("Wallet not found");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(InsufficientWalletBalanceException.class)
    public ProblemDetail handleInsufficientWalletBalanceException(InsufficientWalletBalanceException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/insufficient-wallet-balance"));
        problemDetail.setTitle("Insufficient wallet balance");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ProblemDetail handleInvalidPaymentStateException(InvalidPaymentStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/invalid-payment-state"));
        problemDetail.setTitle("Invalid payment state");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(MissingWalletIdException.class)
    public ProblemDetail handleMissingWalletIdException(MissingWalletIdException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/missing-wallet-id"));
        problemDetail.setTitle("Missing wallet id");
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
