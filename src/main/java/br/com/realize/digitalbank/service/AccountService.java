package br.com.realize.digitalbank.service;

import br.com.realize.digitalbank.api.dto.CreateAccountRequest;
import br.com.realize.digitalbank.domain.Account;
import br.com.realize.digitalbank.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account create(CreateAccountRequest request) {
        try {
            Account account = new Account(request.customerName(), request.initialBalance());
            return accountRepository.save(account);
        } catch (RuntimeException exception) {
            markCurrentTransactionForRollback();
            throw new PersistenceOperationException("Falha ao persistir a conta. Nenhuma alteração deve permanecer gravada.", exception);
        }
    }

    private void markCurrentTransactionForRollback() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Account> findAll(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }
}
