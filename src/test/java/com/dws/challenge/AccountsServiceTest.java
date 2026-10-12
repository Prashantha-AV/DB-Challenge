package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.TransferRequest;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidTransferException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

    @Autowired
    private AccountsService accountsService;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    void addAccount() {
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);

        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    void addAccount_failsOnDuplicateId() {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }
    }

    @Test
    void transferFunds_Success() {
        Account source = new Account("ACC-1", new BigDecimal("1000.00"));
        Account destination = new Account("ACC-2", new BigDecimal("500.00"));

        accountsService.createAccount(source);
        accountsService.createAccount(destination);

        accountsService.transferFunds(new TransferRequest("ACC-1", "ACC-2", new BigDecimal("300.00")));

        assertThat(accountsService.getAccount("ACC-1").getBalance()).isEqualByComparingTo("700.00");
        assertThat(accountsService.getAccount("ACC-2").getBalance()).isEqualByComparingTo("800.00");

        verify(notificationService).notifyAboutTransfer(eq(source), contains("300.00"));
        verify(notificationService).notifyAboutTransfer(eq(destination), contains("300.00"));
    }

    @Test
    void transferFunds_InsufficientBalance() {
        Account source = new Account("ACC-1", new BigDecimal("100.00"));
        Account destination = new Account("ACC-2", new BigDecimal("500.00"));

        accountsService.createAccount(source);
        accountsService.createAccount(destination);

        assertThrows(InsufficientBalanceException.class, () ->
                accountsService.transferFunds(new TransferRequest("ACC-1", "ACC-2", new BigDecimal("150.00"))));

        assertThat(accountsService.getAccount("ACC-1").getBalance()).isEqualByComparingTo("100.00");
        assertThat(accountsService.getAccount("ACC-2").getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void transferFunds_SameAccount() {
        Account source = new Account("ACC-1", new BigDecimal("500.00"));
        accountsService.createAccount(source);

        assertThrows(InvalidTransferException.class, () ->
                accountsService.transferFunds(new TransferRequest("ACC-1", "ACC-1", new BigDecimal("50.00"))));
    }

    @Test
    void transferFunds_NonExistentAccount() {
        Account source = new Account("ACC-1", new BigDecimal("500.00"));
        accountsService.createAccount(source);

        assertThrows(AccountNotFoundException.class, () ->
                accountsService.transferFunds(new TransferRequest("ACC-1", "ACC-999", new BigDecimal("50.00"))));
    }

    @Test
        //Should prevent deadlocks and maintain consistency under concurrent bidirectional transfers
    void transferFunds_ConcurrentTransfers_NoDeadlock() throws InterruptedException {
        Account accA = new Account("ACC-A", new BigDecimal("10000.00"));
        Account accB = new Account("ACC-B", new BigDecimal("10000.00"));

        accountsService.createAccount(accA);
        accountsService.createAccount(accB);

        int numberOfThreads = 40;
        int transfersPerThread = 50;
        BigDecimal transferAmount = new BigDecimal("2.00");

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final boolean forwardDirection = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < transfersPerThread; j++) {
                        if (forwardDirection) {
                            accountsService.transferFunds(new TransferRequest("ACC-A", "ACC-B", transferAmount));
                        } else {
                            accountsService.transferFunds(new TransferRequest("ACC-B", "ACC-A", transferAmount));
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(failureCount.get()).isEqualTo(0);

        BigDecimal finalBalanceA = accountsService.getAccount("ACC-A").getBalance();
        BigDecimal finalBalanceB = accountsService.getAccount("ACC-B").getBalance();

        assertThat(finalBalanceA).isEqualByComparingTo("10000.00");
        assertThat(finalBalanceB).isEqualByComparingTo("10000.00");
    }
}
