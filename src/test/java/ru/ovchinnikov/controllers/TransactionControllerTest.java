package ru.ovchinnikov.controllers;

import org.junit.Before;
import org.junit.Test;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.model.Transaction;
import ru.ovchinnikov.storage.AccountStorage;
import ru.ovchinnikov.storage.TransactionStorage;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static ru.ovchinnikov.controllers.TransactionController.RECHARGE_DESC;
import static ru.ovchinnikov.controllers.TransactionController.WITHDRAW_DESC;

public class TransactionControllerTest {

    private TransactionController controller;
    private AccountStorage accountStorage;
    private TransactionStorage transactionStorage;

    @Before
    public void setUp() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        transactionStorage = new TransactionStorage();
        accountStorage = new AccountStorage();
        controller = new TransactionController(executor, transactionStorage, accountStorage) {
            @Override
            protected long now() {
                return 1;
            }
        };
    }

    @Test
    public void transfer() throws Throwable {
        Account from = Account.create(1, BigDecimal.TEN);
        accountStorage.store(from);
        Account to = Account.create(2, BigDecimal.ONE);
        accountStorage.store(to);
        BigDecimal amount = BigDecimal.ONE;

        controller.transfer(from.id(), to.id(), amount, "");

        assertEquals(from.balance().subtract(amount), accountStorage.findAccountFor(from.id()).get().balance());
        assertEquals(to.balance().add(amount), accountStorage.findAccountFor(to.id()).get().balance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transfer_shouldThrowIllegalArgumentException_whenAmountIsNegative() throws Throwable {
        Account from = Account.create(1, BigDecimal.TEN);
        accountStorage.store(from);
        Account to = Account.create(2, BigDecimal.ONE);
        accountStorage.store(to);
        BigDecimal amount = BigDecimal.ONE.negate();

        controller.transfer(from.id(), to.id(), amount, "");

        fail();
    }

    @Test(expected = IllegalStateException.class)
    public void transfer_shouldThrowIllegalStateException_whenFromAccountIsAbsent() throws Throwable {
        Account to = Account.create(2, BigDecimal.ONE);
        accountStorage.store(to);
        BigDecimal amount = BigDecimal.ONE.negate();

        controller.transfer(1, to.id(), amount, "");

        fail();
    }

    @Test(expected = IllegalStateException.class)
    public void transfer_shouldThrowIllegalStateException_whenToAccountIsAbsent() throws Throwable {
        Account from = Account.create(1, BigDecimal.TEN);
        accountStorage.store(from);
        BigDecimal amount = BigDecimal.ONE.negate();

        controller.transfer(from.id(), 2, amount, "");

        fail();
    }

    @Test(expected = IllegalStateException.class)
    public void getAllTransactions_shouldThrowIllegalStateException_whenAccountIsAbsent() throws Throwable {
        controller.getAllTransactions(1);

        fail();
    }

    @Test
    public void withdraw() throws Throwable {
        Account account = Account.create(1, BigDecimal.TEN);
        accountStorage.store(account);
        BigDecimal amount = BigDecimal.ONE;
        controller.withdraw(account.id(), amount);

        List<Transaction> transactions = controller.getAllTransactions(account.id());

        assertEquals(1, transactions.size());
        assertTrue(transactions.contains(Transaction.create(account.id(), account.id(), amount.negate(), WITHDRAW_DESC, 1)));
        assertEquals(account.balance().subtract(amount), accountStorage.findAccountFor(account.id()).get().balance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void withdraw_shouldThrowIllegalArgumentException_whenAmountIsNegative() throws Throwable {
        Account account = Account.create(1, BigDecimal.TEN);
        accountStorage.store(account);
        controller.withdraw(account.id(), BigDecimal.TEN.negate());

        fail();
    }

    @Test(expected = IllegalStateException.class)
    public void withdraw_shouldThrowIllegalStateException_whenToAccountIsAbsent() throws Throwable {
        controller.withdraw(1, BigDecimal.TEN);

        fail();
    }

    @Test
    public void recharge() throws Throwable {
        Account account = Account.create(1, BigDecimal.TEN);
        accountStorage.store(account);
        BigDecimal amount = BigDecimal.TEN;
        controller.recharge(account.id(), amount);

        List<Transaction> transactions = controller.getAllTransactions(account.id());

        assertEquals(1, transactions.size());
        assertTrue(transactions.contains(Transaction.create(account.id(), account.id(), amount, RECHARGE_DESC, 1)));
        assertEquals(account.balance().add(amount), accountStorage.findAccountFor(account.id()).get().balance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void recharge_shouldThrowIllegalArgumentException_whenAmountIsNegative() throws Throwable {
        Account account = Account.create(1, BigDecimal.TEN);
        accountStorage.store(account);
        controller.recharge(account.id(), BigDecimal.TEN.negate());

        fail();
    }

    @Test(expected = IllegalStateException.class)
    public void recharge_shouldThrowIllegalStateException_whenToAccountIsAbsent() throws Throwable {
        controller.recharge(1, BigDecimal.TEN);

        fail();
    }
}