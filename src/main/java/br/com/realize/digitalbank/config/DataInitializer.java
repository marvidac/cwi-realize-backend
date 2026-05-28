package br.com.realize.digitalbank.config;

import br.com.realize.digitalbank.domain.Account;
import br.com.realize.digitalbank.domain.User;
import br.com.realize.digitalbank.repository.AccountRepository;
import br.com.realize.digitalbank.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    private static final String TEST_USERNAME = "usuario.teste";
    private static final String TEST_PASSWORD = "senha-segura";

    @Bean
    CommandLineRunner preloadData(AccountRepository accountRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        return args -> createInitialData(accountRepository, userRepository, passwordEncoder);
    }

    @Transactional
    void createInitialData(AccountRepository accountRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        createInitialAccounts(accountRepository);
        createTestUser(userRepository, passwordEncoder);
    }

    private void createInitialAccounts(AccountRepository accountRepository) {
        if (accountRepository.count() > 0) {
            return;
        }
        accountRepository.save(new Account("Ana Souza", new BigDecimal("1000.00")));
        accountRepository.save(new Account("Bruno Lima", new BigDecimal("500.00")));
        accountRepository.save(new Account("Carla Martins", new BigDecimal("750.00")));
    }

    private void createTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        if (userRepository.findByUsername(TEST_USERNAME).isPresent()) {
            return;
        }
        userRepository.save(new User(
                "Usuário Teste",
                "usuario.teste@example.com",
                TEST_USERNAME,
                passwordEncoder.encode(TEST_PASSWORD)
        ));
    }
}
