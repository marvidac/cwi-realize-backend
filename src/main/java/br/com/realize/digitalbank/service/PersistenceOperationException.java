package br.com.realize.digitalbank.service;

public class PersistenceOperationException extends RuntimeException {

    public PersistenceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
