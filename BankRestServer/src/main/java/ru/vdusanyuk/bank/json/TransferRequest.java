package ru.vdusanyuk.bank.json;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * the json bean as REST request/inbound message
 */

@XmlRootElement
public class TransferRequest implements Serializable {
     private Long fromAccountNumber;
     private Long toAccountNumber;
     private Long amount;


    public TransferRequest(Long fromAccountNumber, Long toAccountNumber, Long amount) {
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
    }

    //empty constructor is need for JAX-RS serialization
    public TransferRequest() {}

    public Long getFromAccountNumber() {
        return fromAccountNumber;
    }

    public Long getToAccountNumber() {
        return toAccountNumber;
    }

    public Long getAmount() {
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
