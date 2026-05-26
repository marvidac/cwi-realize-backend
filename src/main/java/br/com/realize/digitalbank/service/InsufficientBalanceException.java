package br.com.realize.digitalbank.service;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Saldo insuficiente para realizar a transferência");
    }
}
