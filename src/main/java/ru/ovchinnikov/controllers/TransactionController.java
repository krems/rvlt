package ru.ovchinnikov.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.model.Transaction;
import ru.ovchinnikov.storage.AccountStorage;
import ru.ovchinnikov.storage.TransactionStorage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static ru.ovchinnikov.ConfigConstant.REQUEST_TIMEOUT_SEC;

// @ThreadSafe
public class TransactionController {
    static final Logger log = LogManager.getLogger(TransactionController.class);
    static final String WITHDRAW_DESC = "Withdraw";
    static final String RECHARGE_DESC = "Recharge";
    private final ExecutorService reactorExecutor;
    // @ThreadConfined
    private final TransactionStorage transactionStorage;
    // @ThreadConfined
    private final AccountStorage accountStorage;

    public TransactionController(ExecutorService reactorExecutor,
                                 TransactionStorage transactionStorage,
                                 AccountStorage accountStorage) {
        this.reactorExecutor = reactorExecutor;
        this.transactionStorage = transactionStorage;
        this.accountStorage = accountStorage;
    }

    public void transfer(long from, long to, BigDecimal amount, String description) throws Throwable {
        try {
            CompletableFuture.supplyAsync(() -> {
                doTransfer(from, to, amount, description);
                return null;
            }, reactorExecutor).get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    private void doTransfer(long from, long to, BigDecimal amount, String description) {
        Account fromAccount = validatePresence(from, accountStorage.findAccountFor(from));
        Account toAccount = validatePresence(to, accountStorage.findAccountFor(to));
        if (fromAccount.balance().compareTo(amount) < 0) {
            log.warn("Too few money on {}, to transfer {} to {}", fromAccount, amount, toAccount);
            throw new IllegalArgumentException("Not enough money on " + from);
        }
        if (BigDecimal.ZERO.compareTo(amount) > 0) {
            log.warn("Negative amount {} when transfer from {} to {}", amount, fromAccount, toAccount);
            throw new IllegalArgumentException("Negative amount to transfer");
        }
        Transaction transaction = Transaction.create(fromAccount.id(), toAccount.id(), amount, description, now());
        accountStorage.store(fromAccount.withdraw(amount));
        accountStorage.store(toAccount.recharge(amount));
        transactionStorage.store(transaction);
    }

    private Account validatePresence(long id, Optional<Account> accountHolder) {
        if (!accountHolder.isPresent()) {
            log.warn("Account {} already deleted, can't transfer", id);
            throw new IllegalStateException("Deleted " + id);
        }
        return accountHolder.get();
    }

    public List<Transaction> getAllTransactions(long id) throws Throwable {
        try {
            return CompletableFuture.supplyAsync(() -> {
                if (!accountStorage.findAccountFor(id).isPresent()) {
                    throw new IllegalStateException();
                }
                return transactionStorage.findTransactionsFor(id);
            }, reactorExecutor).get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    public void withdraw(long id, BigDecimal amount) throws Throwable {
        try {
            CompletableFuture.supplyAsync(() -> {
                doWithdraw(id, amount);
                return null;
            }, reactorExecutor).get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    private void doWithdraw(long id, BigDecimal amount) {
        Optional<Account> accountHolder = accountStorage.findAccountFor(id);
        if (!accountHolder.isPresent()) {
            log.warn("Account {} already deleted, can't withdraw", id);
            throw new IllegalStateException("Deleted " + id);
        }
        Account account = accountHolder.get();
        if (account.balance().compareTo(amount) < 0) {
            log.warn("Too few money on {}, to withdraw {}", account, amount);
            throw new IllegalArgumentException("Not enough money on " + id);
        }
        if (BigDecimal.ZERO.compareTo(amount) > 0) {
            log.warn("Negative amount {} when withdrawing account {}", amount, account);
            throw new IllegalArgumentException("Negative amount to withdraw");
        }
        Transaction transaction = Transaction.create(account.id(), account.id(), amount.negate(), WITHDRAW_DESC, now());
        accountStorage.store(account.withdraw(amount));
        transactionStorage.store(transaction);
    }

    public void recharge(long id, BigDecimal amount) throws Throwable {
        try {
            CompletableFuture.supplyAsync(() -> {
                doRecharge(id, amount);
                return null;
            }, reactorExecutor).get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    private void doRecharge(long id, BigDecimal amount) {
        Optional<Account> accountHolder = accountStorage.findAccountFor(id);
        if (!accountHolder.isPresent()) {
            log.warn("Account {} already deleted, can't recharge", id);
            throw new IllegalStateException("Deleted " + id);
        }
        Account account = accountHolder.get();
        if (BigDecimal.ZERO.compareTo(amount) > 0) {
            log.warn("Negative amount {} when recharging account {}", amount, account);
            throw new IllegalArgumentException("Negative amount to recharge");
        }
        Transaction transaction = Transaction.create(account.id(), account.id(), amount, RECHARGE_DESC, now());
        accountStorage.store(account.recharge(amount));
        transactionStorage.store(transaction);
    }

    protected long now() {
        return System.currentTimeMillis();
    }
}
