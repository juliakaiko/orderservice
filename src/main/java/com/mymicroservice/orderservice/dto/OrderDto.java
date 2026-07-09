package com.mymicroservice.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mymicroservice.orderservice.model.enums.OrderStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // ignore unknown fields
public class OrderDto {

    private UUID id;

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OrderStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime creationDate;

    @NotEmpty(message = "OrderItems cannot be empty")
    private Set<OrderItemDto> orderItems = new HashSet<>();
}
