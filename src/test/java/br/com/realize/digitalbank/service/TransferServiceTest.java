package br.com.realize.digitalbank.service;

import br.com.realize.digitalbank.api.dto.TransferRequest;
import br.com.realize.digitalbank.domain.Account;
import br.com.realize.digitalbank.domain.Movement;
import br.com.realize.digitalbank.domain.Transfer;
import br.com.realize.digitalbank.repository.AccountRepository;
import br.com.realize.digitalbank.repository.MovementRepository;
import br.com.realize.digitalbank.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransferRepository transferRepository;
    @Mock private MovementRepository movementRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountRepository, transferRepository, movementRepository, eventPublisher);
    }

    @Test
    void shouldTransferMoneyAndPublishNotificationEvent() {
        Account source = account(1L, "Origem", "100.00");
        Account target = account(2L, "Destino", "10.00");
        when(accountRepository.findAllByIdInOrderByIdAscForUpdate(List.of(1L, 2L))).thenReturn(List.of(source, target));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            ReflectionTestUtils.setField(transfer, "id", 99L);
            return transfer;
        });

        Transfer transfer = transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("30.00")));

        assertThat(transfer.getId()).isEqualTo(99L);
        assertThat(source.getBalance()).isEqualByComparingTo("70.00");
        assertThat(target.getBalance()).isEqualByComparingTo("40.00");
        verify(movementRepository, times(2)).save(any(Movement.class));
        verify(eventPublisher).publishEvent(new TransferCompletedEvent(99L, 1L, 2L));
    }

    @Test
    void shouldRejectTransferWhenBalanceIsInsufficient() {
        Account source = account(1L, "Origem", "10.00");
        Account target = account(2L, "Destino", "10.00");
        when(accountRepository.findAllByIdInOrderByIdAscForUpdate(List.of(1L, 2L))).thenReturn(List.of(source, target));

        assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("30.00"))))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(transferRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 1L, new BigDecimal("10.00"))))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Conta de origem e destino devem ser diferentes");
    }

    @Test
    void shouldWrapPersistenceFailureAndNotPublishEvent() {
        Account source = account(1L, "Origem", "100.00");
        Account target = account(2L, "Destino", "10.00");
        when(accountRepository.findAllByIdInOrderByIdAscForUpdate(List.of(1L, 2L))).thenReturn(List.of(source, target));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            ReflectionTestUtils.setField(transfer, "id", 99L);
            return transfer;
        });
        when(movementRepository.save(any(Movement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new RuntimeException("falha simulada na persistência"));

        assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("30.00"))))
                .isInstanceOf(PersistenceOperationException.class)
                .hasMessage("Falha ao persistir a transferência. Nenhuma alteração deve permanecer gravada.")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldPersistDebitAndCreditMovements() {
        Account source = account(1L, "Origem", "100.00");
        Account target = account(2L, "Destino", "10.00");
        when(accountRepository.findAllByIdInOrderByIdAscForUpdate(List.of(1L, 2L))).thenReturn(List.of(source, target));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            ReflectionTestUtils.setField(transfer, "id", 99L);
            return transfer;
        });
        ArgumentCaptor<Movement> movementCaptor = ArgumentCaptor.forClass(Movement.class);

        transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("30.00")));

        verify(movementRepository, times(2)).save(movementCaptor.capture());
        assertThat(movementCaptor.getAllValues()).extracting(Movement::getType)
                .containsExactly(br.com.realize.digitalbank.domain.MovementType.DEBIT, br.com.realize.digitalbank.domain.MovementType.CREDIT);
        assertThat(movementCaptor.getAllValues()).extracting(Movement::getBalanceAfter)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("70.00"), new BigDecimal("40.00"));
    }

    private Account account(Long id, String name, String balance) {
        Account account = new Account(name, new BigDecimal(balance));
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
