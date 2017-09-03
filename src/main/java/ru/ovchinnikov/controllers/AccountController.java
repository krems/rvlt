package ru.ovchinnikov.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.storage.AccountStorage;

import java.util.List;
import java.util.Optional;

public class AccountController {
    private static final Logger log = LogManager.getLogger(AccountController.class);
    private final static AccountController instance = new AccountController();
    private final AccountStorage storage = new AccountStorage();

    public Account createAccount() {
        Account account = Account.create();
        storage.store(account);
        return account;
    }

    public Optional<Account> findAccount(long id) {
        return storage.findAccountFor(id);
    }

    public List<Account> getAccounts() {
        return storage.getAllAccounts();
    }

    public boolean deleteAccount(long id) {
        return storage.remove(id);
    }

    public static AccountController instance() {
        return instance;
    }
}
