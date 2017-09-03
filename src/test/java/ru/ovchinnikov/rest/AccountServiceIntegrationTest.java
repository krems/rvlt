package ru.ovchinnikov.rest;

import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.ovchinnikov.Main;
import ru.ovchinnikov.model.Account;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

public class AccountServiceIntegrationTest {

    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        server = Main.startServer();
        Client c = ClientBuilder.newClient();
        target = c.target(Main.BASE_URI).path(AccountService.ACCOUNTS);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    @Test
    public void testCreateAccount_shouldCreateNewAccount() {
        Response responseMsg = target.request(MediaType.APPLICATION_JSON).post(Entity.json(""));
        assertEquals(Response.Status.CREATED.getStatusCode(), responseMsg.getStatus());
    }

    @Test
    public void testGetAccounts_shouldReturnEmptyAccounts_ifNonePresent() {
        List<Account> accounts = target.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Account>>() {
        });
        assertTrue(accounts.isEmpty());
    }

    @Test
    public void testGetAccounts_shouldReturnAccounts_ifPresent() {
        getAccounts(0);
        createAccount();

        List<Account> accounts = target.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Account>>() {
        });

        assertEquals(1, accounts.size());
    }

    @Test
    public void testCreateAccount_shouldCreateNewAccount_eachTimeItsCalled() {
        getAccounts(0);

        createAccount();
        getAccounts(1);

        createAccount();

        List<Account> accounts = target.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Account>>() {
        });
        assertEquals(2, accounts.size());
    }

    @Test
    public void testDeleteAccount_shouldDeleteRequestedAccount_ifPresent() {
        getAccounts(0);
        createAccount();
        createAccount();
        List<Account> accounts = getAccounts(2);
        Account accountToDelete = accounts.get(0);
        String id = String.valueOf(accountToDelete.id());

        Response deleteResponse = target.path(id).request(MediaType.APPLICATION_JSON).delete();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
        accounts = getAccounts(1);
        assertFalse(accounts.contains(accountToDelete));
    }

    @Test
    public void testDeleteAccount_shouldResponseWithError_ifNotPresent() {
        getAccounts(0);
        createAccount();
        createAccount();
        List<Account> accounts = getAccounts(2);
        Account accountToDelete = accounts.get(0);
        String id = String.valueOf(accountToDelete.id() + 7);

        Response deleteResponse = target.path(id).request(MediaType.APPLICATION_JSON).delete();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), deleteResponse.getStatus());
        accounts = target.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Account>>() {
        });
        assertEquals(2, accounts.size());
    }

    @Test
    public void testGetAccount_shouldReturnAccount_ifPresent() {
        getAccounts(0);
        createAccount();
        createAccount();
        List<Account> accounts = getAccounts(2);
        Account accountToRetrieve = accounts.get(0);
        String id = String.valueOf(accountToRetrieve.id());

        Account account = target.path(id).request(MediaType.APPLICATION_JSON).get(Account.class);

        assertEquals(accountToRetrieve.id(), account.id());
    }

    @Test
    public void testGetAccount_shouldResponseWithError_ifNotPresent() {
        getAccounts(0);
        createAccount();
        Response response = target.path("/0").request(MediaType.APPLICATION_JSON).get();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private List<Account> getAccounts(int value) {
        List<Account> accounts = target.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Account>>() {
        });
        assumeThat(accounts.size(), Is.is(value));
        return accounts;
    }

    private void createAccount() {
        Response responseMsg = target.request(MediaType.APPLICATION_JSON).post(Entity.json(""));
        assumeThat(responseMsg.getStatus(), Is.is(Response.Status.CREATED.getStatusCode()));
    }
}
