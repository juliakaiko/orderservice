package com.mymicroservice.orderservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Value("${spring.application.name}")
    private String serviceName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .orElse(UUID.randomUUID().toString());

        MDC.put(REQUEST_ID, requestId);
        MDC.put("serviceName", serviceName);

        response.setHeader(REQUEST_ID_HEADER, requestId);

        /**
         * Write a log to the trace file at the beginning of the request
         */
        log.info("{} {}",
                request.getMethod(),
                request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            /**
             * Write the response log to the trace file
             */
            log.info("Response status: {}", response.getStatus());

            MDC.clear();
        }
    }
}
