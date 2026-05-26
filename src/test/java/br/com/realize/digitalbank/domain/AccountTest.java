package br.com.realize.digitalbank.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void shouldDebitWhenBalanceIsEnough() {
        Account account = new Account("Cliente", new BigDecimal("100.00"));

        account.debit(new BigDecimal("35.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("64.50");
    }

    @Test
    void shouldNotDebitWhenBalanceIsInsufficient() {
        Account account = new Account("Cliente", new BigDecimal("20.00"));

        assertThatThrownBy(() -> account.debit(new BigDecimal("20.01")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Saldo insuficiente");
        assertThat(account.getBalance()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldCreditBalance() {
        Account account = new Account("Cliente", new BigDecimal("20.00"));

        account.credit(new BigDecimal("10.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("30.00");
    }
}
