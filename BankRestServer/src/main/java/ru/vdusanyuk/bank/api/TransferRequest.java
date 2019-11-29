package ru.vdusanyuk.bank.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * the json bean as REST request/inbound message
 */

@XmlRootElement
public class TransferRequest implements Serializable {
     private long fromAccountNumber;
     private long toAccountNumber;
     private long amount;


    public TransferRequest(long fromAccountNumber, long toAccountNumber, long amount) {
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
    }

    //empty constructor is need for JAX-RS serialization
    public TransferRequest() {}

    public long getFromAccountNumber() {
        return fromAccountNumber;
    }

    public long getToAccountNumber() {
        return toAccountNumber;
    }

    public long getAmount() {
        return amount;
    }

    public void setFromAccountNumber(long fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public void setToAccountNumber(long toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
