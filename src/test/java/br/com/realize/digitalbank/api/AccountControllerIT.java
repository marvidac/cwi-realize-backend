package br.com.realize.digitalbank.api;

import br.com.realize.digitalbank.domain.Account;
import br.com.realize.digitalbank.domain.User;
import br.com.realize.digitalbank.repository.AccountRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new User(
                "Usuário Teste",
                "usuario.teste@example.com",
                "usuario.teste",
                passwordEncoder.encode("senha-segura")
        ));

        accountRepository.save(new Account("Ana Souza", new BigDecimal("1000.00")));
        accountRepository.save(new Account("Bruno Lima", new BigDecimal("500.00")));
    }

    @Test
    void shouldListAccountsUsingValidSortParameter() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get("/api/accounts")
                        .param("sort", "id,asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldReturn400WhenSwaggerSendsSortAsJsonArrayString() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get("/api/accounts")
                        .param("sort", "[\"id\"]")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value(
                        "Ordenacao invalida. Use um campo de Account, por exemplo: sort=id,asc ou sort=customerName,asc"
                ));
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
