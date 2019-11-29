package ru.vdusanyuk.bank.json;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * the json bean as REST response/outbound message
 */

@XmlRootElement
public class ServiceResponse implements Serializable {
    private String responseStatus;
    private String errorMessage;
    private Long accountNumber;
    private Long balance;

    /**
     * Zero-args contructor
     */
    public ServiceResponse() {}

    public ServiceResponse(String responseStatus, String errorMessage, Long acntNum, Long balance) {
        this.responseStatus = responseStatus;
        this.errorMessage = errorMessage;
        this.accountNumber = acntNum;
        this.balance = balance;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(Long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "ServiceResponse{" +
                "responseStatus='" + responseStatus + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", accountNumber=" + accountNumber +
                ", balance=" + balance +
                '}';
    }
}
