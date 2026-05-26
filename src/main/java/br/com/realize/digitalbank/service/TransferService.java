package br.com.realize.digitalbank.service;

import br.com.realize.digitalbank.api.dto.TransferRequest;
import br.com.realize.digitalbank.domain.*;
import br.com.realize.digitalbank.repository.AccountRepository;
import br.com.realize.digitalbank.repository.MovementRepository;
import br.com.realize.digitalbank.repository.TransferRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final MovementRepository movementRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           MovementRepository movementRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Transfer transfer(TransferRequest request) {
        validateRequest(request);

        List<Account> lockedAccounts = accountRepository.findAllByIdInOrderByIdAscForUpdate(
                List.of(request.sourceAccountId(), request.targetAccountId())
        );

        Map<Long, Account> accountsById = lockedAccounts.stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        Account source = accountsById.get(request.sourceAccountId());
        Account target = accountsById.get(request.targetAccountId());

        if (source == null) throw new AccountNotFoundException(request.sourceAccountId());
        if (target == null) throw new AccountNotFoundException(request.targetAccountId());

        try {
            source.debit(request.amount());
        } catch (IllegalStateException exception) {
            throw new InsufficientBalanceException();
        }
        target.credit(request.amount());

        Transfer transfer = transferRepository.save(new Transfer(source, target, request.amount()));
        movementRepository.save(new Movement(source, transfer, MovementType.DEBIT, request.amount(), source.getBalance()));
        movementRepository.save(new Movement(target, transfer, MovementType.CREDIT, request.amount(), target.getBalance()));

        eventPublisher.publishEvent(new TransferCompletedEvent(transfer.getId(), source.getId(), target.getId()));
        return transfer;
    }

    private void validateRequest(TransferRequest request) {
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new InvalidTransferException("Conta de origem e destino devem ser diferentes");
        }
    }
}
