package com.turkcell.identity.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFoundException(UserNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/user-not-found"));
        problemDetail.setTitle("User not found");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ProblemDetail handleRoleNotFoundException(RoleNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/role-not-found"));
        problemDetail.setTitle("Role not found");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ProblemDetail handlePermissionNotFoundException(PermissionNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/permission-not-found"));
        problemDetail.setTitle("Permission not found");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ProblemDetail handleDuplicateUsernameException(DuplicateUsernameException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/duplicate-username"));
        problemDetail.setTitle("Username already in use");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmailException(DuplicateEmailException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/duplicate-email"));
        problemDetail.setTitle("Email already in use");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(DuplicateRoleNameException.class)
    public ProblemDetail handleDuplicateRoleNameException(DuplicateRoleNameException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/duplicate-role-name"));
        problemDetail.setTitle("Role name already in use");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(DuplicatePermissionCodeException.class)
    public ProblemDetail handleDuplicatePermissionCodeException(DuplicatePermissionCodeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/duplicate-permission-code"));
        problemDetail.setTitle("Permission code already in use");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentialsException(InvalidCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/invalid-credentials"));
        problemDetail.setTitle("Invalid credentials");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshTokenException(InvalidRefreshTokenException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setType(URI.create("https://telco.example/errors/invalid-refresh-token"));
        problemDetail.setTitle("Invalid refresh token");
        addCommonProperties(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        // Not: @PreAuthorize reddi (method-security), Spring Security'nin filter-chain seviyesindeki
        // AccessDeniedHandler'ini degil, buradaki handler'i tetikler - cunku istek DispatcherServlet'e
        // zaten ulasmis, hata controller metod cagrisi sirasinda firlatiliyor. Bu yuzden §12.1 formati
        // (instance/correlationId, timestamp yok) burada da uygulanir.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        problemDetail.setType(URI.create("https://telco.example/errors/access-denied"));
        problemDetail.setTitle("Access denied");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            problemDetail.setProperty("correlationId", correlationId);
        }

        return problemDetail;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
        problemDetail.setType(URI.create("https://telco.example/errors/unauthorized"));
        problemDetail.setTitle("Unauthorized");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            problemDetail.setProperty("correlationId", correlationId);
        }

        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
        problem.setType(URI.create("https://telco.example/errors/malformed-request"));
        problem.setTitle("Malformed request body");
        problem.setInstance(URI.create(request.getRequestURI()));

        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }

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
        log.error("Unexpected error", ex);
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
