package br.com.realize.digitalbank.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long sourceAccountId,
        @NotNull Long targetAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
