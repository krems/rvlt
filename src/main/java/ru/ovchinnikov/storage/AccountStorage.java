package ru.ovchinnikov.storage;

import ru.ovchinnikov.model.Account;

import java.util.*;

public class AccountStorage {
    private final Map<Long, Account> store = new HashMap<>();

    public void store(Account account) {
        store.put(account.id(), account);
    }

    public Optional<Account> findAccountFor(long id) {
        Account account = store.get(id);
        return Optional.ofNullable(account);
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(store.values());
    }

    public boolean remove(long id) {
        return store.remove(id) != null;
    }
}
