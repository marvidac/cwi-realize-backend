package br.com.realize.digitalbank.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String customerName,
        @NotNull @DecimalMin(value = "0.00") BigDecimal initialBalance
) {
}
