package ru.ovchinnikov.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ovchinnikov.controllers.AccountController;
import ru.ovchinnikov.model.Account;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Path(AccountService.ACCOUNTS)
public class AccountService {
    public static final String ACCOUNTS = "/accounts";
    private static final Logger log = LogManager.getLogger(AccountService.class);
    private final AccountController accountController = AccountController.instance();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Account> getAccounts() {
        return accountController.getAccounts();
    }

    @POST
    public Response createAccount() {
        Account account = accountController.createAccount();
        URI location = buildNewAccountLocation(account);
        return Response.created(location).build();
    }

    private URI buildNewAccountLocation(Account account) {
        try {
            return new URI(AccountService.ACCOUNTS + "/" + account.id());
        } catch (URISyntaxException e) {
            log.error("Couldn't create URI for new {}", account, e);
            return URI.create("");
        }
    }
}
