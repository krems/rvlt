package ru.ovchinnikov.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ovchinnikov.controllers.AccountController;
import ru.ovchinnikov.controllers.TransactionController;
import ru.ovchinnikov.model.Account;
import ru.ovchinnikov.model.Transaction;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Path(AccountService.ACCOUNTS + "/{id}")
public class TransactionService {
    public static final String TRANSFER = "/transfer";
    public static final String RECHARGE = "/recharge";
    public static final String WITHDRAW = "/withdraw";
    public static final String TRANSACTIONS = "/transactions";
    private static final Logger log = LogManager.getLogger(TransactionService.class);
    private final AccountController accountController = AccountController.instance();
    private final TransactionController transactionController = TransactionController.instance();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount(@PathParam("id") long id) {
        Optional<Account> account = accountController.findAccount(id);
        if (account.isPresent()) {
            return account.get();
        }
        log.debug("Requested non-existent account for id {}", id);
        throw new NotFoundException("Account for id " + id + " not found");
    }

    @DELETE
    public Response deleteAccount(@PathParam("id") long id) {
        if (!accountController.deleteAccount(id)) {
            log.debug("Requested deletion of non-existent account for id {}", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Path(TRANSFER)
    public Response transfer(@PathParam("id") long from,
                             @QueryParam("to") long to,
                             @QueryParam("amount") BigDecimal amount,
                             @DefaultValue("") @QueryParam("desc") String description) {
        Optional<Account> fromAccount = accountController.findAccount(from);
        Optional<Account> toAccount = accountController.findAccount(to);
        if (fromAccount.isPresent() && toAccount.isPresent()) {
            if (transactionController.transfer(fromAccount.get(), toAccount.get(), amount, description)) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }
        log.debug("Requested transaction between accounts, one doen'r exist, ids from:{}, tp:{}", from, to);
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path(RECHARGE)
    public Response recharge(@PathParam("id") long id,
                             @QueryParam("amount") BigDecimal amount) {
        Optional<Account> account = accountController.findAccount(id);
        if (!account.isPresent()) {
            log.debug("Requested recharge of non-existent account for id {}", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (transactionController.recharge(account.get(), amount)) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_MODIFIED).build();
    }

    @POST
    @Path(WITHDRAW)
    public Response withdraw(@PathParam("id") long id,
                             @QueryParam("amount") BigDecimal amount) {
        Optional<Account> account = accountController.findAccount(id);
        if (!account.isPresent()) {
            log.debug("Requested withdrawal of non-existent account for id {}", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (transactionController.withdraw(account.get(), amount)) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_MODIFIED).build();
    }

    @GET
    @Path(TRANSACTIONS)
    public List<Transaction> listTransactions(@PathParam("id") long id) {
        return transactionController.getAllTransactions(id);
    }
}
