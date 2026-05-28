package br.com.realize.digitalbank.service;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Usuário ou senha inválidos");
    }
}
