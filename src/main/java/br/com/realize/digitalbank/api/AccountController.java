package br.com.realize.digitalbank.api;

import br.com.realize.digitalbank.api.dto.AccountResponse;
import br.com.realize.digitalbank.api.dto.CreateAccountRequest;
import br.com.realize.digitalbank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma nova conta")
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accountService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma conta por ID")
    public AccountResponse findById(@PathVariable Long id) {
        return AccountResponse.from(accountService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista contas cadastradas")
    public Page<AccountResponse> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return accountService.findAll(pageable).map(AccountResponse::from);
    }
}
