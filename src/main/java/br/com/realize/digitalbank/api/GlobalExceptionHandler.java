package br.com.realize.digitalbank.api;

import br.com.realize.digitalbank.api.dto.ErrorResponse;
import br.com.realize.digitalbank.service.AccountNotFoundException;
import br.com.realize.digitalbank.service.InsufficientBalanceException;
import br.com.realize.digitalbank.service.InvalidCredentialsException;
import br.com.realize.digitalbank.service.InvalidTransferException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(AccountNotFoundException exception) {
        return ErrorResponse.of(404, "Not Found", List.of(exception.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException exception) {
        return ErrorResponse.of(401, "Unauthorized", List.of(exception.getMessage()));
    }

    @ExceptionHandler({InsufficientBalanceException.class, InvalidTransferException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleBusiness(RuntimeException exception) {
        return ErrorResponse.of(422, "Unprocessable Entity", List.of(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ErrorResponse.of(400, "Bad Request", messages);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidSort(PropertyReferenceException exception) {
        return ErrorResponse.of(400, "Bad Request", List.of(
                "Ordenacao invalida. Use um campo de Account, por exemplo: sort=id,asc ou sort=customerName,asc"
        ));
    }
}
