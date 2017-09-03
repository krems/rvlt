package ru.ovchinnikov.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ovchinnikov.Injector;
import ru.ovchinnikov.controllers.AccountController;
import ru.ovchinnikov.model.Account;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeoutException;


// todo: document API
@Path(AccountService.ACCOUNTS)
public class AccountService {
    public static final String ACCOUNTS = "/accounts";
    private static final Logger log = LogManager.getLogger(AccountService.class);
    private final AccountController accountController = Injector.accountController();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Account> getAccounts() {
        try {
            return accountController.getAccounts();
        } catch (InterruptedException e) {
            throw new WebApplicationException("Interrupted exception");
        } catch (TimeoutException e) {
            throw new ServiceUnavailableException("Timeout processing request");
        } catch (Throwable throwable) {
            throw new InternalServerErrorException();
        }
    }

    @POST
    public Response createAccount() {
        try {
            Account account = accountController.createAccount();
            URI location = buildNewAccountLocation(account.id());
            return Response.created(location).build();
        } catch (TimeoutException e) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private URI buildNewAccountLocation(long id) {
        try {
            return new URI(AccountService.ACCOUNTS + "/" + id);
        } catch (URISyntaxException e) {
            log.error("Couldn't create URI for new account id={}", id, e);
            return URI.create("");
        }
    }
}
