package br.com.realize.digitalbank.api;

import br.com.realize.digitalbank.domain.Account;
import br.com.realize.digitalbank.domain.User;
import br.com.realize.digitalbank.repository.AccountRepository;
import br.com.realize.digitalbank.repository.MovementRepository;
import br.com.realize.digitalbank.repository.NotificationRepository;
import br.com.realize.digitalbank.repository.TransferRepository;
import br.com.realize.digitalbank.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AccountRepository accountRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private TransferRepository transferRepository;
    @Autowired private UserRepository userRepository;

    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        movementRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new User(
                "Usuário Teste",
                "usuario.teste@example.com",
                "usuario.teste",
                passwordEncoder.encode("senha-segura")
        ));

        source = accountRepository.save(new Account("Origem", new BigDecimal("100.00")));
        target = accountRepository.save(new Account("Destino", new BigDecimal("50.00")));
    }

    @Test
    void shouldLoginAndTransferMoneyBetweenAccounts() throws Exception {
        String token = loginAndGetToken();
        String payload = """
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 25.00
                }
                """.formatted(source.getId(), target.getId());

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
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
        String token = loginAndGetToken();
        String payload = """
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 125.00
                }
                """.formatted(source.getId(), target.getId());

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages[0]").value("Saldo insuficiente para realizar a transferência"));
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        String loginPayload = """
                {
                  "username": "usuario.teste",
                  "password": "senha-incorreta"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0]").value("Usuário ou senha inválidos"));
    }

    @Test
    void shouldReturn401WhenTokenIsMissing() throws Exception {
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0]").value("Token de autenticação inválido ou ausente"));
    }

    private String loginAndGetToken() throws Exception {
        String loginPayload = """
                {
                  "username": "usuario.teste",
                  "password": "senha-segura"
                }
                """;

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }
}
