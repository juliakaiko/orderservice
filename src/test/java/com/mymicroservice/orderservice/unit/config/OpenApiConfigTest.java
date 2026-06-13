package com.mymicroservice.orderservice.unit.config;

import com.mymicroservice.orderservice.config.OpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void openAPI_ShouldExposeBearerSecurityScheme_WhenBeanIsCreated() {
        OpenAPI openAPI = openApiConfig.openAPI();

        assertNotNull(openAPI.getInfo());
        assertEquals("Order Servise API", openAPI.getInfo().getTitle());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("Bearer Authentication"));
        assertEquals(1, openAPI.getSecurity().size());
    }
}
