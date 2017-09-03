package ru.ovchinnikov.controllers;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.storage.AccountStorage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

public class AccountControllerTest {

    private AccountController controller;

    @Before
    public void setUp() throws Exception {
        controller = new AccountController(Executors.newSingleThreadExecutor(), new AccountStorage());
    }

    @Test
    public void createAccount() throws Throwable {
        List<Account> accounts = controller.getAccounts();
        assumeThat(accounts.size(), Is.is(0));

        Account account = controller.createAccount();

        accounts = controller.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(account, accounts.get(0));
    }

    @Test
    public void findAccount() throws Throwable {
        List<Account> accounts = controller.getAccounts();
        assumeThat(accounts.size(), Is.is(0));
        Account account = controller.createAccount();
        accounts = controller.getAccounts();
        assumeThat(accounts.size(), Is.is(1));
        assumeThat(accounts.get(0), Is.is(account));

        Optional<Account> accountOptional = controller.findAccount(account.id());

        assertTrue(accountOptional.isPresent());
        assertEquals(account, accountOptional.get());
    }

    @Test
    public void findAccount_shouldReturnEmptyOptional_whenNoAccountFound() throws Throwable {
        List<Account> accounts = controller.getAccounts();
        assumeThat(accounts.size(), Is.is(0));
        Account account = controller.createAccount();
        accounts = controller.getAccounts();
        assumeThat(accounts.size(), Is.is(1));
        assumeThat(accounts.get(0), Is.is(account));

        Optional<Account> accountOptional = controller.findAccount(account.id() + 5);

        assertFalse(accountOptional.isPresent());
    }

    @Test
    public void deleteAccount() throws Throwable {
        List<Account> accounts = controller.getAccounts();
        assumeThat(accounts.size(), Is.is(0));
        Account account = controller.createAccount();
        accounts = controller.getAccounts();
        assumeThat(accounts.size(), Is.is(1));
        assumeThat(accounts.get(0), Is.is(account));

        assertTrue(controller.deleteAccount(account.id()));
    }

    @Test
    public void deleteAccount_shouldReturnFalse_ifAccountWithIdProvidedIsNotPresent() throws Throwable {
        List<Account> accounts = controller.getAccounts();
        assumeThat(accounts.size(), Is.is(0));

        assertFalse(controller.deleteAccount(1));
    }
}