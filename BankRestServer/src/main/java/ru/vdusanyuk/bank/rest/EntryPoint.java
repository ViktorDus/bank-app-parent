package ru.vdusanyuk.bank.rest;

import ru.vdusanyuk.bank.json.ServiceResponse;
import ru.vdusanyuk.bank.json.TransferRequest;
import ru.vdusanyuk.bank.dao.BankHolder;
import ru.vdusanyuk.bank.dao.model.OperationResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

/**
 * REST rest for bank inter account transfer
 */

@Path("/bankService")
public class EntryPoint {

    private static final String ERROR_ACCOUNT_NOT_FOUND = "Account Not Found.";
    private static final String ERROR_BALANCE_NOT_ENOUGH = "Balance Not Enough.";
    private static final String ERROR_INVALID_REQUEST = "Invalid Request.";

    private BankHolder bankHolder = BankHolder.getInstance();

    @GET
    @Path("/total")
    @Produces(MediaType.TEXT_HTML)
    public String getTotal() {
        return bankHolder.getTotalBalance().toString();
    }

    @GET
    @Path("/account/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    public ServiceResponse getAccountState(@PathParam("param") Long acntNumber) {
        OperationResult result = bankHolder.getAccount(acntNumber);

        return generateResponse(result);
    }

    @POST
    @Path("/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    public ServiceResponse postTransferMoney(TransferRequest request) {
        return validateRequest(request)
                .orElseGet(() -> generateResponse(
                      bankHolder.submitTransfer(request.getFromAccountNumber(),
                                                request.getToAccountNumber(),
                                                request.getAmount())
                    )
                );
    }

    @GET
    @Path("/transfer")
    @Produces(MediaType.APPLICATION_JSON)
    public ServiceResponse getTransferMoney(@QueryParam("fromAccountNumber") Long fromAcntNumber,
                                            @QueryParam("toAccountNumber") Long toAcntNumber,
                                            @QueryParam("amount") Long amount ) {
        return validateRequest(fromAcntNumber, toAcntNumber, amount)
                .orElseGet(() -> generateResponse(
                        bankHolder.submitTransfer(fromAcntNumber,
                                                  toAcntNumber,
                                                  amount)
                    )
                );
    }

    private Optional<ServiceResponse> validateRequest(Long fromAcntNumber, Long toAcntNumber, Long amount) {
        return Optional.ofNullable(nullSafeLong(amount) <= 0 || nullSafeLong(fromAcntNumber) <= 0
               || nullSafeLong(toAcntNumber) <= 0 || nullSafeLong(fromAcntNumber) == nullSafeLong(toAcntNumber) ?
               new ServiceResponse("ERROR", ERROR_INVALID_REQUEST, null,null) :
               null);
    }

    private Optional<ServiceResponse> validateRequest(TransferRequest request) {
        return validateRequest(request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());
    }

    private ServiceResponse generateResponse(OperationResult result) {
        return result.getCode() == 0
                ? new ServiceResponse("SUCCESS",null, result.getAccountNumber(), result.getBalance())
                : generateErrorResponse(result.getBalance());
    }

    private ServiceResponse generateErrorResponse(Long newBalance) {
        return new ServiceResponse("ERROR",
                newBalance == null ? ERROR_ACCOUNT_NOT_FOUND : ERROR_BALANCE_NOT_ENOUGH,
                null, newBalance);
    }

    private long nullSafeLong(Long value) {
        return value == null ? 0L : value;
    }

}
