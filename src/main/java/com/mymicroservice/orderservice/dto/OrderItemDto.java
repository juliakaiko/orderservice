package com.mymicroservice.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // ignore unknown fields
public class OrderItemDto {

    private Long id;

    @JsonProperty("orderId")
    @NotNull(message = "Order ID cannot be null")
    private Long orderId;

    /**
     * Unique identifier of the Item associated with the OrderItem.
     * <p>
     * Participates in serialization (object to JSON conversion) and deserialization
     * (JSON to object conversion) under the name {@code "itemId"}.
     * <p>
     * When mapping ({@code Mapping}), the ID of the associated Item is used.
     */
    @JsonProperty("itemId")
    @NotNull(message = "Item ID cannot be null")
    private Long itemId;

    @NotNull(message = "Quantity cannot be null")
    private Long quantity;
}
