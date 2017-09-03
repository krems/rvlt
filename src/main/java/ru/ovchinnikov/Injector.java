package ru.ovchinnikov;

import ru.ovchinnikov.controllers.AccountController;
import ru.ovchinnikov.controllers.TransactionController;

public class Injector {
    private static volatile AccountController accountController;
    private static volatile TransactionController transactionController;

    private Injector() {
    }

    public static void init(AccountController accountController, TransactionController transactionController) {
        Injector.accountController = accountController;
        Injector.transactionController = transactionController;
    }

    public static AccountController accountController() {
        return accountController;
    }

    public static TransactionController transactionController() {
        return transactionController;
    }
}
