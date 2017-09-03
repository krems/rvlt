package ru.ovchinnikov.storage;

import ru.ovchinnikov.model.Transaction;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.List;

public class TransactionStorage {
    private final MultivaluedMap<Long, Transaction> store = new MultivaluedHashMap<>();

    public void store(Transaction transaction) {
        long from = transaction.from();
        store.add(from, transaction);
        long to = transaction.to();
        if (from != to) {
            store.add(to, transaction);
        }
    }

    public List<Transaction> findTransactionsFor(long id) {
        List<Transaction> transactions = store.get(id);
        return transactions == null ? Collections.emptyList() : transactions;
    }
}
