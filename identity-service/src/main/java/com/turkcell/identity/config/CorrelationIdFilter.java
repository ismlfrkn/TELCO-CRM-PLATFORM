package com.turkcell.identity.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Spring Security'nin kendi filter zinciri (springSecurityFilterChain) varsayilan olarak bu
 * filtreden once calisir (SecurityProperties.DEFAULT_FILTER_ORDER = HIGHEST_PRECEDENCE + 100).
 * Auth reddi (401/403) durumunda istek hic buraya ulasmadan RestAuthenticationEntryPoint /
 * RestAccessDeniedHandler tarafindan kesilir ve MDC hic doldurulmamis olurdu - bu yuzden bu filtre
 * en yuksek onceligi alir ki correlationId, auth hatalarinda da garanti bicimde uretilmis/MDC'de olsun.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER_NAME = "Correlation-Id";
    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_LOG_VAR_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_LOG_VAR_NAME);
        }
    }
}
