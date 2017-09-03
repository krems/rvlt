package ru.ovchinnikov.rest;

import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.ovchinnikov.Main;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.model.Transaction;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static ru.ovchinnikov.rest.TransactionService.*;

public class TransactionServiceIntegrationTest {
    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        server = Main.startServer();
        Client c = ClientBuilder.newClient();
        target = c.target(Main.BASE_URI + AccountService.ACCOUNTS);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    @Test
    public void testRecharge_shouldRechargeAccount_whenAccountExists() {
        long id = createAccount();
        BigDecimal balance = new BigDecimal(100);

        Response response = target.path(String.valueOf(id) + RECHARGE).queryParam("amount", balance)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Account account = getAccount(id);
        assertEquals(balance, account.balance());
    }

    @Test
    public void testRecharge_shouldReturnError_whenAccountDoesntExist() {
        BigDecimal balance = new BigDecimal(100);

        Response response = target.path("1" + RECHARGE).queryParam("amount", balance)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testWithdraw_shouldWithdrawAccount_whenBalanceIsPositive() {
        long id = createAccount();
        BigDecimal balance = new BigDecimal(100);
        recharge(id, balance);
        BigDecimal withdraw = new BigDecimal(50.3);

        Response withDrawResponse = target.path(String.valueOf(id) + WITHDRAW).queryParam("amount", withdraw)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.OK.getStatusCode(), withDrawResponse.getStatus());
        Account account = getAccount(id);
        assertEquals(balance.subtract(withdraw), account.balance());
    }

    @Test
    public void testWithdraw_shouldReturnError_whenBalanceIsNotPositive() {
        long id = createAccount();
        BigDecimal withdraw = new BigDecimal(50.3);

        Response withDrawResponse = target.path(String.valueOf(id) + WITHDRAW).queryParam("amount", withdraw)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), withDrawResponse.getStatus());
    }

    @Test
    public void testWithdraw_shouldReturnError_whenBalanceIsNotEnoughMoney() {
        long id = createAccount();
        BigDecimal balance = new BigDecimal(10);
        recharge(id, balance);
        BigDecimal withdraw = new BigDecimal(50.3);

        Response withDrawResponse = target.path(String.valueOf(id) + WITHDRAW).queryParam("amount", withdraw)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), withDrawResponse.getStatus());
    }

    @Test
    public void testWithdraw_shouldReturnError_whenAccountDoesntExist() {
        Response withDrawResponse = target.path("1" + WITHDRAW).queryParam("amount", new BigDecimal(100))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), withDrawResponse.getStatus());
    }

    @Test
    public void testTransfer_shouldTransferMoneyBetweenAccounts_whenStateIsValid() {
        long from = createAccount();
        long to = createAccount();
        recharge(from, new BigDecimal(100));
        recharge(to, new BigDecimal(100));

        Response transferResponse = target.path(String.valueOf(from) + TRANSFER)
                .queryParam("to", to)
                .queryParam("amount", new BigDecimal(100))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.OK.getStatusCode(), transferResponse.getStatus());
        Account fromAccount = getAccount(from);
        assertEquals(BigDecimal.ZERO, fromAccount.balance());
        Account toAccount = getAccount(to);
        assertEquals(BigDecimal.valueOf(200), toAccount.balance());
    }

    @Test
    public void testTransfer_shouldReturnError_whenBalanceStateIsInvalid() {
        long from = createAccount();
        long to = createAccount();
        recharge(from, new BigDecimal(10));
        recharge(to, new BigDecimal(100));

        Response transferResponse = target.path(String.valueOf(from) + TRANSFER)
                .queryParam("to", to)
                .queryParam("amount", new BigDecimal(100))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), transferResponse.getStatus());
    }

    @Test
    public void testTransfer_shouldReturnError_whenFromAccountDoesntExist() {
        long to = createAccount();
        recharge(to, new BigDecimal(100));

        Response transferResponse = target.path(String.valueOf(to + 100) + TRANSFER)
                .queryParam("to", to)
                .queryParam("amount", new BigDecimal(100))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), transferResponse.getStatus());
    }

    @Test
    public void testTransfer_shouldReturnError_whenToAccountDoesntExist() {
        long from = createAccount();
        recharge(from, new BigDecimal(100));

        Response transferResponse = target.path(String.valueOf(from) + TRANSFER)
                .queryParam("to", from + 100)
                .queryParam("amount", new BigDecimal(100))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), transferResponse.getStatus());
    }

    @Test
    public void testGetTransactions_shouldReturnTransactions_whenAccountExists() {
        long from = createAccount();
        long to = createAccount();
        recharge(from, new BigDecimal(100));
        recharge(to, new BigDecimal(100));
        transfer(from, to, BigDecimal.TEN, "ten");
        transfer(to, from, BigDecimal.ONE, "one back");

        List<Transaction> transactions = target.path(String.valueOf(from) + TRANSACTIONS)
                .request(MediaType.APPLICATION_JSON).get(new GenericType<List<Transaction>>() {
                });

        assertEquals(3, transactions.size());
        assertTrue(transactions.contains(Transaction.create(from, to, BigDecimal.TEN, "ten", 1)));
        assertTrue(transactions.contains(Transaction.create(to, from, BigDecimal.ONE, "one back", 1)));
    }

    @Test
    public void testGetTransactions_shouldReturnEmptyList_whenAccountDoesntHaveTransactions() {
        long from = createAccount();

        List<Transaction> transactions = target.path(String.valueOf(from) + TRANSACTIONS)
                .request(MediaType.APPLICATION_JSON).get(new GenericType<List<Transaction>>() {
                });

        assertTrue(transactions.isEmpty());
    }

    @Test
    public void testGetTransactions_shouldReturnError_whenAccountDoesntExist() {
        Response transferResponse = target.path("1" + TRANSACTIONS).request(MediaType.APPLICATION_JSON).get();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), transferResponse.getStatus());
    }

    private void transfer(long from, long to, BigDecimal amount, String description) {
        Response transferResponse = target.path(String.valueOf(from) + TRANSFER)
                .queryParam("to", to)
                .queryParam("amount", amount)
                .queryParam("desc", description)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));

        assumeThat(transferResponse.getStatus(), Is.is(Response.Status.OK.getStatusCode()));
    }

    private void recharge(long id, BigDecimal balance) {
        Response rechargeResponse = target.path(String.valueOf(id) + RECHARGE).queryParam("amount", balance)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""));
        assumeThat(rechargeResponse.getStatus(), Is.is(Response.Status.OK.getStatusCode()));
        Account account = getAccount(id);
        assumeThat(account.balance(), Is.is(balance));
    }

    private long createAccount() {
        Response responseMsg = target.path("").request(MediaType.APPLICATION_JSON).post(Entity.json(""));
        assumeThat(responseMsg.getStatus(), Is.is(Response.Status.CREATED.getStatusCode()));
        String uri = responseMsg.getHeaderString("Location");
        return Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
    }

    private Account getAccount(long id) {
        return target.path(String.valueOf(id)).request(MediaType.APPLICATION_JSON).get(Account.class);
    }
}
