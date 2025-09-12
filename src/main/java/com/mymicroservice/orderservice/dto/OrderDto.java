package com.mymicroservice.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mymicroservice.orderservice.model.OrderStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // ignore unknown fields
public class OrderDto {

    private Long id;

    @NotNull
    private Long userId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OrderStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate creationDate;

    @NotEmpty
    private Set<OrderItemDto> orderItems = new HashSet<>();
}
