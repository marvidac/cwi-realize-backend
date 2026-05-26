package br.com.realize.digitalbank.config;

import br.com.realize.digitalbank.domain.Account;
import br.com.realize.digitalbank.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner preloadAccounts(AccountRepository accountRepository) {
        return args -> createInitialAccounts(accountRepository);
    }

    @Transactional
    void createInitialAccounts(AccountRepository accountRepository) {
        if (accountRepository.count() > 0) {
            return;
        }
        accountRepository.save(new Account("Ana Souza", new BigDecimal("1000.00")));
        accountRepository.save(new Account("Bruno Lima", new BigDecimal("500.00")));
        accountRepository.save(new Account("Carla Martins", new BigDecimal("750.00")));
    }
}
