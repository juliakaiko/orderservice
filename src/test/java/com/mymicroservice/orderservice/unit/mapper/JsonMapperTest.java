package com.mymicroservice.orderservice.unit.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.orderservice.mapper.JsonMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMapperTest {

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        jsonMapper = new JsonMapper(new ObjectMapper(), validator);
    }

    @Test
    void toJson_ShouldReturnEmpty_WhenObjectIsNull() {
        Optional<String> result = jsonMapper.toJson(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void toJson_ShouldSerializeObject_WhenObjectIsValid() {
        OrderEventDto dto = OrderEventDto.builder()
                .orderId("1")
                .userId("1")
                .paymentAmount(BigDecimal.TEN)
                .build();

        Optional<String> result = jsonMapper.toJson(dto);

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("\"orderId\":\"1\""));
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenJsonIsBlank() {
        Optional<OrderEventDto> result = jsonMapper.fromJson(" ", OrderEventDto.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void fromJson_ShouldDeserializeObject_WhenJsonIsValid() {
        String json = "{\"orderId\":\"1\",\"userId\":\"1\",\"paymentAmount\":10}";

        Optional<OrderEventDto> result = jsonMapper.fromJson(json, OrderEventDto.class);

        assertTrue(result.isPresent());
        assertEqualsSafe("1", result.get().getOrderId());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenJsonIsInvalid() {
        Optional<OrderEventDto> result = jsonMapper.fromJson("{invalid", OrderEventDto.class);

        assertFalse(result.isPresent());
    }

    private void assertEqualsSafe(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
