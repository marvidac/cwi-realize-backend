package br.com.realize.digitalbank.service;

import br.com.realize.digitalbank.domain.*;
import br.com.realize.digitalbank.repository.AccountRepository;
import br.com.realize.digitalbank.repository.NotificationRepository;
import br.com.realize.digitalbank.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final NotificationRepository notificationRepository;

    public NotificationService(AccountRepository accountRepository,
                               TransferRepository transferRepository,
                               NotificationRepository notificationRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.notificationRepository = notificationRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyTransferCompleted(TransferCompletedEvent event) {
        Account targetAccount = accountRepository.findById(event.targetAccountId())
                .orElseThrow(() -> new AccountNotFoundException(event.targetAccountId()));
        Transfer transfer = transferRepository.findById(event.transferId())
                .orElseThrow(() -> new IllegalArgumentException("Transferência não encontrada: " + event.transferId()));

        String message = "Transferência recebida com sucesso no valor de R$ " + transfer.getAmount();
        notificationRepository.save(new Notification(targetAccount, transfer, message, NotificationStatus.SENT));
        LOGGER.info("Notificação enviada para conta {}: {}", targetAccount.getId(), message);
    }
}
