package ru.ovchinnikov.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.model.Transaction;
import ru.ovchinnikov.storage.TransactionStorage;

import java.math.BigDecimal;
import java.util.List;

public class TransactionController {
    private static final Logger log = LogManager.getLogger(TransactionController.class);
    private static final String WITHDRAW_DESC = "Withdraw";
    private static final String RECHARGE_DESC = "Recharge";
    private static TransactionController instance = new TransactionController();
    private final TransactionStorage storage = new TransactionStorage();

    public boolean transfer(Account from, Account to, BigDecimal amount, String description) {
        if (from.balance().compareTo(amount) < 0) {
            log.warn("Too few money on {}, to transfer {} to {}", from, amount, to);
            return false;
        }
        if (BigDecimal.ZERO.compareTo(amount) > 0) {
            log.warn("Negative amount {} when transfer from {} to {}", amount, from, to);
            return false;
        }
        Transaction transaction = Transaction.create(from.id(), to.id(), amount, description, now());
        from.withdraw(amount);
        to.recharge(amount);
        storage.store(transaction);
        return true;
    }

    public List<Transaction> getAllTransactions(long id) {
        return storage.findTransactionsFor(id);
    }

    public boolean withdraw(Account account, BigDecimal amount) {
        if (account.balance().compareTo(amount) < 0) {
            log.warn("Too few money on {}, to withdraw {}", account, amount);
            return false;
        }
        if (BigDecimal.ZERO.compareTo(amount) > 0) {
            log.warn("Negative amount {} when withdrawing account {}", amount, account);
            return false;
        }
        Transaction transaction = Transaction.create(account.id(), account.id(), amount.negate(), WITHDRAW_DESC, now());
        account.withdraw(amount);
        storage.store(transaction);
        return true;
    }

    public boolean recharge(Account account, BigDecimal amount) {
        if (BigDecimal.ZERO.compareTo(amount) > 0) {
            log.warn("Negative amount {} when recharging account {}", amount, account);
            return false;
        }
        Transaction transaction = Transaction.create(account.id(), account.id(), amount, RECHARGE_DESC, now());
        account.withdraw(amount);
        storage.store(transaction);
        return true;
    }

    protected long now() {
        return System.currentTimeMillis();
    }

    public static TransactionController instance() {
        return instance;
    }
}
