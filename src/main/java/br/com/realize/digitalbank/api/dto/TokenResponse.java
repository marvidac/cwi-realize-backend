package br.com.realize.digitalbank.api.dto;

import java.time.OffsetDateTime;

public record TokenResponse(
        String accessToken,
        String tokenType,
        OffsetDateTime expiresAt
) {
}
