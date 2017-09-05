import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.ovchinnikov.Main;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.rest.AccountService;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static ru.ovchinnikov.rest.TransactionService.RECHARGE;
import static ru.ovchinnikov.rest.TransactionService.TRANSFER;

public class StressTest {
    private static final double EPSILON = 1e-15;
    private static final int THREADS = 8;
    private static final int ITERATIONS = 1000;
    private ExecutorService executorService;
    private HttpServer server;

    @Before
    public void setUp() {
        executorService = new ForkJoinPool(THREADS);
        server = Main.startServer();
    }

    @After
    public void tearDown() {
        executorService.shutdownNow();
        server.shutdownNow();
    }

    @Ignore
    @Test
    public void moneyTransferStressTest() throws ExecutionException, InterruptedException {
        WebTarget target = buildTargetClient();
        createAccounts(target);
        List<Account> accounts = getAccounts(target);
        BigDecimal balance = BigDecimal.valueOf(1000);
        accounts.forEach(account -> recharge(target, account.id(), balance));
        BigDecimal total = balance.multiply(BigDecimal.valueOf(accounts.size()));

        List<Future<?>> futures = fireRandomTransfers(accounts, balance);
        await(futures);

        List<Account> finalAccounts = getAccounts(target);
        assertEquals(3, finalAccounts.size());
        checkTotal(total, finalAccounts);
    }

    private void createAccounts(WebTarget target) {
        for (int i = 0; i < 3; i++) {
            createAccount(target);
        }
    }

    private void checkTotal(BigDecimal total, List<Account> finalAccounts) {
        BigDecimal finalTotal = BigDecimal.ZERO;
        for (Account account : finalAccounts) {
            finalTotal = finalTotal.add(account.balance());
        }
        assertEquals(EPSILON, total.doubleValue(), finalTotal.doubleValue());
    }

    private List<Future<?>> fireRandomTransfers(List<Account> accounts, BigDecimal balance) {
        CountDownLatch latch = new CountDownLatch(THREADS);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            futures.add(executorService.submit(() -> performRandomTransfer(accounts, balance, latch)));
        }
        return futures;
    }

    private void await(List<Future<?>> futures) throws InterruptedException, ExecutionException {
        for (Future<?> future : futures) {
            future.get();
        }
    }

    private void performRandomTransfer(List<Account> accounts, BigDecimal balance, CountDownLatch latch) {
        List<Account> localAccounts = new ArrayList<>(accounts);
        WebTarget client = buildTargetClient();
        awaitOthers(latch);
        for (int i = 0; i < ITERATIONS; i++) {
            Collections.shuffle(localAccounts);
            Account from = localAccounts.get(0);
            Account to = localAccounts.get(1);
            double amount = ThreadLocalRandom.current().nextDouble(balance.doubleValue());
            transfer(client, from.id(), to.id(),
                    BigDecimal.valueOf(amount), String.valueOf(from.id()) + "->" + String.valueOf(to.id()));
        }
    }

    private void awaitOthers(CountDownLatch latch) {
        latch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private WebTarget buildTargetClient() {
        return ClientBuilder.newClient().target(Main.BASE_URI).path(AccountService.ACCOUNTS);
    }

    private void createAccount(WebTarget target) {
        target.request(MediaType.APPLICATION_JSON).post(Entity.json(""));
    }

    private List<Account> getAccounts(WebTarget target) {
        return target.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Account>>() {
        });
    }

    private void recharge(WebTarget target, long id, BigDecimal balance) {
        target.path(String.valueOf(id) + RECHARGE).queryParam("amount", balance)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));
    }

    private void transfer(WebTarget target, long from, long to, BigDecimal amount, String description) {
        target.path(String.valueOf(from) + TRANSFER)
                .queryParam("to", to)
                .queryParam("amount", amount)
                .queryParam("desc", description)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

    }
}
