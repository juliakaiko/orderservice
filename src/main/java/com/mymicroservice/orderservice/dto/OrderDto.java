package com.mymicroservice.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mymicroservice.orderservice.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // ignore unknown fields
public class OrderDto {

    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    private OrderStatus status;

    @NotNull
    @PastOrPresent(message = "Creation date must be in the past or present")
   // @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) //ISO.DATE = yyyy-MM-dd
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate creationDate;

}
