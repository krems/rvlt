package ru.ovchinnikov.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ovchinnikov.Injector;
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
import java.util.concurrent.TimeoutException;

// todo: document API
@Path(AccountService.ACCOUNTS + "/{id}")
public class TransactionService {
    public static final String TRANSFER = "/transfer";
    public static final String RECHARGE = "/recharge";
    public static final String WITHDRAW = "/withdraw";
    public static final String TRANSACTIONS = "/transactions";
    private static final Logger log = LogManager.getLogger(TransactionService.class);
    private final AccountController accountController = Injector.accountController();
    private final TransactionController transactionController = Injector.transactionController();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount(@PathParam("id") long id) {
        try {
            Optional<Account> account = accountController.findAccount(id);
            if (account.isPresent()) {
                return account.get();
            }
        } catch (InterruptedException e) {
            throw new WebApplicationException("Interrupted exception");
        } catch (TimeoutException e) {
            throw new ServiceUnavailableException("Timeout processing request");
        } catch (Throwable throwable) {
            throw new InternalServerErrorException();
        }
        log.debug("Requested non-existent account for id {}", id);
        throw new NotFoundException("Account for id " + id + " not found");
    }

    @DELETE
    public Response deleteAccount(@PathParam("id") long id) {
        try {
            if (!accountController.deleteAccount(id)) {
                log.debug("Requested deletion of non-existent account for id {}", id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.noContent().build();
        } catch (TimeoutException e) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT).build();
        } catch (Throwable throwable) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path(TRANSFER)
    public Response transfer(@PathParam("id") long from,
                             @QueryParam("to") long to,
                             @QueryParam("amount") BigDecimal amount,
                             @DefaultValue("") @QueryParam("desc") String description) {
        try {
            transactionController.transfer(from, to, amount, description);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (TimeoutException e) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT).build();
        } catch (Throwable throwable) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path(RECHARGE)
    public Response recharge(@PathParam("id") long id,
                             @QueryParam("amount") BigDecimal amount) {
        try {
            transactionController.recharge(id, amount);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (TimeoutException e) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT).build();
        } catch (Throwable throwable) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path(WITHDRAW)
    public Response withdraw(@PathParam("id") long id,
                             @QueryParam("amount") BigDecimal amount) {
        try {
            transactionController.withdraw(id, amount);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (TimeoutException e) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT).build();
        } catch (Throwable throwable) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path(TRANSACTIONS)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Transaction> listTransactions(@PathParam("id") long id) {
        try {
            return transactionController.getAllTransactions(id);
        } catch (IllegalStateException e) {
            log.debug("Requested list of transactions for non-existent account with id {}", id);
            throw new NotFoundException("Account for id " + id + " not found");
        } catch (InterruptedException e) {
            throw new WebApplicationException("Interrupted exception");
        } catch (TimeoutException e) {
            throw new ServiceUnavailableException("Timeout processing request");
        } catch (Throwable throwable) {
            throw new InternalServerErrorException();
        }
    }
}
