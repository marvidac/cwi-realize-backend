package br.com.realize.digitalbank.api;

import br.com.realize.digitalbank.domain.Account;
import br.com.realize.digitalbank.repository.AccountRepository;
import br.com.realize.digitalbank.repository.MovementRepository;
import br.com.realize.digitalbank.repository.NotificationRepository;
import br.com.realize.digitalbank.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private TransferRepository transferRepository;

    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        movementRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        source = accountRepository.save(new Account("Origem", new BigDecimal("100.00")));
        target = accountRepository.save(new Account("Destino", new BigDecimal("50.00")));
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() throws Exception {
        String payload = """
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 25.00
                }
                """.formatted(source.getId(), target.getId());

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceAccountId").value(source.getId()))
                .andExpect(jsonPath("$.targetAccountId").value(target.getId()))
                .andExpect(jsonPath("$.amount").value(25.00));

        Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account updatedTarget = accountRepository.findById(target.getId()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo("75.00");
        assertThat(updatedTarget.getBalance()).isEqualByComparingTo("75.00");
        assertThat(movementRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldReturn422WhenBalanceIsInsufficient() throws Exception {
        String payload = """
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 125.00
                }
                """.formatted(source.getId(), target.getId());

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages[0]").value("Saldo insuficiente para realizar a transferência"));
    }
}
