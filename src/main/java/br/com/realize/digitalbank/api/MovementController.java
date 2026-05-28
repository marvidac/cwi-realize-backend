package br.com.realize.digitalbank.api;

import br.com.realize.digitalbank.api.dto.MovementResponse;
import br.com.realize.digitalbank.config.OpenApiConfig;
import br.com.realize.digitalbank.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts/{accountId}/movements")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    @Operation(summary = "Consulta movimentações financeiras de uma conta")
    public Page<MovementResponse> findByAccount(@PathVariable Long accountId,
                                                @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return movementService.findByAccount(accountId, pageable).map(MovementResponse::from);
    }
}
