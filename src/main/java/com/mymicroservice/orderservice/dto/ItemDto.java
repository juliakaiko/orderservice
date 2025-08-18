package com.mymicroservice.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // ignore unknown fields
public class ItemDto {

    private Long id;

    @NotBlank(message = "Item name cannot be blank")
    @Size(max = 100, message = "Item name must be less than 100 characters")
    private String name;

    @NotNull
    @Positive(message = "The price must be greater than 0")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal price;
}
