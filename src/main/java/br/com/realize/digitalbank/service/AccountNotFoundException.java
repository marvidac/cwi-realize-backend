package br.com.realize.digitalbank.service;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long accountId) {
        super("Conta não encontrada: " + accountId);
    }
}
