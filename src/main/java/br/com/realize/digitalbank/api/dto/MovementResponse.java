package br.com.realize.digitalbank.api.dto;

import br.com.realize.digitalbank.domain.Movement;
import br.com.realize.digitalbank.domain.MovementType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MovementResponse(
        Long id,
        Long accountId,
        Long transferId,
        MovementType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        OffsetDateTime createdAt
) {
    public static MovementResponse from(Movement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getAccount().getId(),
                movement.getTransfer().getId(),
                movement.getType(),
                movement.getAmount(),
                movement.getBalanceAfter(),
                movement.getCreatedAt()
        );
    }
}
