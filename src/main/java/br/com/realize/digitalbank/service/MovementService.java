package br.com.realize.digitalbank.service;

import br.com.realize.digitalbank.domain.Movement;
import br.com.realize.digitalbank.repository.AccountRepository;
import br.com.realize.digitalbank.repository.MovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovementService {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    public MovementService(AccountRepository accountRepository, MovementRepository movementRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public Page<Movement> findByAccount(Long accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return movementRepository.findByAccountId(accountId, pageable);
    }
}
