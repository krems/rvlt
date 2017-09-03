package ru.ovchinnikov;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import ru.ovchinnikov.controllers.AccountController;
import ru.ovchinnikov.controllers.TransactionController;
import ru.ovchinnikov.storage.AccountStorage;
import ru.ovchinnikov.storage.TransactionStorage;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {
    public static final String BASE_URI = "http://localhost:8080/revolut";
    private final static ExecutorService reactorExecutor = Executors.newSingleThreadExecutor();

    public static HttpServer startServer() {
        ResourceConfig rc = bootstrapServer();
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    private static ResourceConfig bootstrapServer() {
        AccountStorage accountStorage = new AccountStorage();
        TransactionStorage transactionStorage = new TransactionStorage();
        AccountController accountController = new AccountController(reactorExecutor, accountStorage);
        TransactionController transactionController =
                new TransactionController(reactorExecutor, transactionStorage, accountStorage);
        Injector.init(accountController, transactionController);
        return new ResourceConfig().packages("ru.ovchinnikov");
    }

    public static void main(String[] args) {
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%s/application.wadl\nHit enter to stop it...", BASE_URI));
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reactorExecutor.shutdownNow();
            server.shutdownNow();
        }
    }
}

