package ru.vdusanyuk.bank.dao.model;

/**
 *  bean encapsulating complex result from service calls
 */
public class OperationResult {
    /**
     * result code : 0 - success, 1 - error
     */
    private final int code;

    /**
     * operation account
     */
    private final Long accountNumber;

    /**
     * amount indicating result balance
     */
    private final Long balance;

    /**
     * error text
     */
    private final String errorMessage;

    /**
     * constructor
     * @param code result code: 0 - success, 1 - error
     * @param errorMessage error mesage in case of error
     * @param accountNum main account number involved in operation
     * @param newBalance balance of the account after operation
     */
    public OperationResult(int code, String errorMessage, Long accountNum, Long newBalance) {
        this.code = code;
        this.errorMessage = errorMessage;
        this.accountNumber = accountNum;
        this.balance = newBalance;
    }


    public int getCode() {
        return code;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }


    public Long getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return "OperationResult{" +
                "code=" + code +
                ", accountNumber=" + accountNumber +
                ", balance=" + balance +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
