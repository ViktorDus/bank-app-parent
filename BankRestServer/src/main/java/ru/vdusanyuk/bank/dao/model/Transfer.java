package ru.vdusanyuk.bank.dao.model;

import java.util.concurrent.atomic.AtomicReference;

/**
 *  bean - entity representing money transfer task (transaction)
 */
public class Transfer {
    private final long id;
    private final Account fromAccount;
    private final Account toAccount;
    private final long amount;
    private final AtomicReference<TransferStatus> status;

    public Transfer(long id, Account fromAccount, Account toAccount, long amount) {
        this.id = id;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        status = new AtomicReference<>(TransferStatus.DRAFT);
    }

    /** gettter for transaction id (could be used for logging, sorting etc.)*/
    public long getId() {
        return id;
    }

    /** gettter for 'from' (withdraw) account {@link Account} */
    public Account getFromAccount() {
        return fromAccount;
    }

    /** gettter for 'to' (deposit) account {@link Account} */
    public Account getToAccount() {
        return toAccount;
    }

    /** gettter for amount of money to transfer */
    public long getAmount() {
        return amount;
    }

    /**
     * check status
     * @return true if status not PENDING and not DRAFT
     */
    public boolean isProcessed() {
        return status.get().isProcessed();
    }
    /**
     * check status
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return status.get().isPending();
    }

    public void setStatus(TransferStatus status) {
        this.status.set(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transfer transfer = (Transfer) o;

        if (id != transfer.id) return false;
        if (!fromAccount.equals(transfer.fromAccount)) return false;
        return toAccount.equals(transfer.toAccount);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + fromAccount.hashCode();
        result = 31 * result + toAccount.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "id=" + id +
                ", fromAccount=" + fromAccount.getAccountNumber() +
                ", toAccount=" + toAccount.getAccountNumber() +
                ", amount=" + amount +
                ", status=" + status.get() +
                '}';
    }
}
