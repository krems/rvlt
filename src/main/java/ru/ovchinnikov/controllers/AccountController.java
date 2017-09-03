package ru.ovchinnikov.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.storage.AccountStorage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static ru.ovchinnikov.ConfigConstant.REQUEST_TIMEOUT_SEC;

// @ThreadSafe
public class AccountController {
    private static final Logger log = LogManager.getLogger(AccountController.class);
    private final ExecutorService reactorExecutor;
    // @ThreadConfined
    private final AccountStorage storage;

    public AccountController(ExecutorService reactorExecutor, AccountStorage storage) {
        this.reactorExecutor = reactorExecutor;
        this.storage = storage;
    }

    public Account createAccount() throws Throwable {
        try {
            return CompletableFuture.supplyAsync(() -> {
                Account account = Account.create();
                storage.store(account);
                return account;
            }, reactorExecutor).get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    public Optional<Account> findAccount(long id) throws Throwable {
        try {
            return CompletableFuture.supplyAsync(() -> storage.findAccountFor(id), reactorExecutor)
                    .get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    public List<Account> getAccounts() throws Throwable {
        try {
            return CompletableFuture.supplyAsync(storage::getAllAccounts, reactorExecutor)
                    .get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    public boolean deleteAccount(long id) throws Throwable {
        try {
            return CompletableFuture.supplyAsync(() -> storage.remove(id), reactorExecutor)
                    .get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}
