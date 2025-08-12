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
    private Long orderId;

    /**
     * Unique identifier of the Item associated with the OrderItem.
     * <p>
     * Participates in serialization (object to JSON conversion) and deserialization
     * (JSON to object conversion) under the name {@code "itemId"}.
     * <p>
     * When mapping ({@code Mapping}), the ID of the associated User is used.
     */
    @JsonProperty("itemId")
    private Long itemId;

    @NotNull
    private Long quantity;
}
