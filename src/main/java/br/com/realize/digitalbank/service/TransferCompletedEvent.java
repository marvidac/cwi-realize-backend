package br.com.realize.digitalbank.service;

public record TransferCompletedEvent(Long transferId, Long sourceAccountId, Long targetAccountId) {
}
