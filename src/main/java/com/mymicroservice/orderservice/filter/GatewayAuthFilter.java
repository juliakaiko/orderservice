package com.mymicroservice.orderservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String internalCall = request.getHeader("X-Internal-Call");
        String sourceService = request.getHeader("X-Source-Service");

        System.out.println("!!!! Authorization = " + authHeader);
        System.out.println("!!!! internalCall = " + internalCall);
        System.out.println("!!!! sourceService = " + sourceService);

        // Internal call from Gateway
        if ("true".equals(internalCall) && "GATEWAY".equals(sourceService)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "INTERNAL_GATEWAY",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        //  // JWT Base64 parsing, without signature verification (Gateway does it), to extract roles
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String[] parts = token.split("\\.");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Invalid JWT structure");
                }

                // Decoding the payload
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

                String userId = (String) claims.get("sub");
                List<String> roles = (List<String>) claims.get("roles");

                var authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();

                System.out.println("!!!! Authorities = " + authorities);

                var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                System.out.println("Failed to parse JWT: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

}
