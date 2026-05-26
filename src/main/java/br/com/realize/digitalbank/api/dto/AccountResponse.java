package br.com.realize.digitalbank.api.dto;

import br.com.realize.digitalbank.domain.Account;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountResponse(
        Long id,
        String customerName,
        BigDecimal balance,
        OffsetDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getCustomerName(), account.getBalance(), account.getCreatedAt());
    }
}
