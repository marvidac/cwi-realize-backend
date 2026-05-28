package br.com.realize.digitalbank.api;

import br.com.realize.digitalbank.api.dto.TransferRequest;
import br.com.realize.digitalbank.api.dto.TransferResponse;
import br.com.realize.digitalbank.config.OpenApiConfig;
import br.com.realize.digitalbank.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Realiza transferência de valores entre contas")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return TransferResponse.from(transferService.transfer(request));
    }
}
