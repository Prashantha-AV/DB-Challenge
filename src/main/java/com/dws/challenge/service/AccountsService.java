package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.TransferRequest;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    private final NotificationService notificationService;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        Account account = this.accountsRepository.getAccount(accountId);
        if (account == null) {
            throw new AccountNotFoundException("Account with ID " + accountId + " does not exist.");
        }
        return account;
    }

    /**
     * Executes thread-safe fund transfer between accounts.
     * Enforces ordered lock on account IDs to eliminate deadlock cycles.
     */
    public void transferFunds(TransferRequest transferRequest) {
        String fromId = transferRequest.getAccountFromId();
        String toId = transferRequest.getAccountToId();
        BigDecimal amount = transferRequest.getAmount();

        if (fromId.trim().equalsIgnoreCase(toId.trim())) {
            throw new InvalidTransferException("Debit and credit account identifiers must be distinct.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be strictly positive.");
        }

        Account sourceAccount = getAccount(fromId);
        Account destinationAccount = getAccount(toId);

        // Deterministic lock acquisition order avoids deadlocks under concurrent cross-transfers
        Account firstLock = sourceAccount.getAccountId().compareTo(destinationAccount.getAccountId()) < 0
                ? sourceAccount : destinationAccount;
        Account secondLock = firstLock == sourceAccount ? destinationAccount : sourceAccount;

        firstLock.getAccountLock().lock();
        try {
            secondLock.getAccountLock().lock();
            try {
                if (sourceAccount.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientBalanceException(
                            String.format("Account %s has insufficient funds (Current: %s, Requested: %s).",
                                    fromId, sourceAccount.getBalance(), amount));
                }

                sourceAccount.debit(amount);
                destinationAccount.credit(amount);

                log.info("Completed transfer of {} from account {} to account {}", amount, fromId, toId);
            } finally {
                secondLock.getAccountLock().unlock();
            }
        } finally {
            firstLock.getAccountLock().unlock();
        }

        // Notification dispatch occurs outside the lock window to prevent I/O blocking
        sendTransferNotifications(sourceAccount, destinationAccount, amount);
    }

    private void sendTransferNotifications(Account source, Account destination, BigDecimal amount) {
        try {
            notificationService.notifyAboutTransfer(
                    source,
                    String.format("Transfer debited: %s sent to account %s.", amount, destination.getAccountId())
            );
        } catch (Exception ex) {
            log.error("Failed to notify sender account: {}", source.getAccountId(), ex);
        }

        try {
            notificationService.notifyAboutTransfer(
                    destination,
                    String.format("Transfer credited: %s received from account %s.", amount, source.getAccountId())
            );
        } catch (Exception ex) {
            log.error("Failed to notify receiver account: {}", destination.getAccountId(), ex);
        }
    }
}