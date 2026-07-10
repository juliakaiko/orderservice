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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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

    @Test
    void fromJson_ShouldReturnEmpty_WhenBytesAreNull() {
        Optional<OrderEventDto> result = jsonMapper.fromJson((byte[]) null, OrderEventDto.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenBytesAreEmpty() {
        Optional<OrderEventDto> result = jsonMapper.fromJson(new byte[0], OrderEventDto.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void fromJson_ShouldDeserializeFromBytes_WhenJsonIsValid() {
        byte[] json = "{\"orderId\":\"1\",\"userId\":\"1\",\"paymentAmount\":10}".getBytes();

        Optional<OrderEventDto> result = jsonMapper.fromJson(json, OrderEventDto.class);

        assertTrue(result.isPresent());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenBytesAreInvalid() {
        Optional<OrderEventDto> result = jsonMapper.fromJson("{invalid".getBytes(), OrderEventDto.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void toJson_ShouldReturnEmpty_WhenSerializationFails() {
        Optional<String> result = jsonMapper.toJson(new UnserializableObject());

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fromJson_ShouldReturnEmpty_WhenValidationFails() {
        jakarta.validation.Validator validator = org.mockito.Mockito.mock(jakarta.validation.Validator.class);
        jakarta.validation.ConstraintViolation<OrderEventDto> violation =
                org.mockito.Mockito.mock(jakarta.validation.ConstraintViolation.class);
        jakarta.validation.Path path = org.mockito.Mockito.mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("orderId");
        when(violation.getPropertyPath()).thenReturn(path);
        Set violations = Set.of(violation);
        when(validator.validate(org.mockito.ArgumentMatchers.any())).thenReturn(violations);
        JsonMapper mapperWithMock = new JsonMapper(new ObjectMapper(), validator);

        Optional<OrderEventDto> result = mapperWithMock.fromJson(
                "{\"orderId\":\"1\",\"userId\":\"1\",\"paymentAmount\":10}", OrderEventDto.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenUnexpectedExceptionOccurs() throws Exception {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingMapper.readValue(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(OrderEventDto.class)))
                .thenThrow(new RuntimeException("unexpected"));
        JsonMapper mapperWithMock = new JsonMapper(failingMapper, Validation.buildDefaultValidatorFactory().getValidator());

        Optional<OrderEventDto> result = mapperWithMock.fromJson("{\"orderId\":\"1\"}", OrderEventDto.class);

        assertTrue(result.isEmpty());
    }

    private static class UnserializableObject {
        public UnserializableObject getSelf() {
            return this;
        }
    }

    private void assertEqualsSafe(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
