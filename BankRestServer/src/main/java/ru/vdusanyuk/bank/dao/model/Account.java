package ru.vdusanyuk.bank.dao.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Bank account entity
 */
public class Account {
    /**
     * account unique id
      */
    private final long accountNumber;
    /*
    * account balance saved, not imcluding pending and draft transactions
     */
    private final AtomicLong savedBalance;

    /**
     * pending transactions stored in this collection till asynch batch processing
     */
    private final Map<Transfer, Integer> pendingTransactions = new ConcurrentHashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * constructor with args:
     * @param acntNumber account number
     * @param balance initial balance
     */
    public Account(long acntNumber, long balance) {
        this.accountNumber = acntNumber;
        this.savedBalance = new AtomicLong(balance);
    }

    /**
     * getter
     */
    public long getAccountNumber() {
        return accountNumber;
    }

     /**
     * getter, saved balance
     *
     * @return  balance
     */
    public long getSavedBalance() {
            return savedBalance.get();
    }

    /**
     * calculate real balance as sum of saved balance and pending transfer parts
     * @param trxStamp  nano time stamp or -1 if no stamp required
     * @param includeDraft include draft transactions
     * @return calculated balance
     */
    public long getStampedBalance(long trxStamp, boolean includeDraft) {
        readLock.lock();
        try {
            long pendingAmt = pendingTransactions.entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getKey().isProcessed() &&
                                 (includeDraft || e.getKey().isPending()) &&
                                 (trxStamp <= 0 || e.getKey().getId() < trxStamp))
                    .mapToLong(e -> e.getValue() * e.getKey().getAmount())
                    .sum();
            return savedBalance.get() + pendingAmt;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * add new pending Transfer to collection
     * @param transfer the transfer object {@link Transfer}
     * @param isWithdraw - the  flag of withdraw operation
     * @return true if transfer is passed validation
     */
    public OperationResult addPendingTransaction(Transfer transfer, boolean isWithdraw) {
        if (transfer.isProcessed()) {
            return new OperationResult(1, "Transfer is Processed status", accountNumber, -1L);
        }
        readLock.lock();
        try {
            long oldBalance = getStampedBalance(-1, true );
            //validate transfer amount for  withdraw operation
            if (isWithdraw && oldBalance < transfer.getAmount()) {
                transfer.setStatus(TransferStatus.ERROR);
                return new OperationResult(1, "Transfer amount exceeds balance", accountNumber, oldBalance);
            }
            int sign = isWithdraw ? -1 : 1;
            pendingTransactions.put(transfer, sign);
            return new OperationResult(0, null, accountNumber,
                    oldBalance + sign * transfer.getAmount());
        } finally {
          readLock.unlock();
        }
    }

    /**
     * the task for apply pending transfers/transactions and save new balance
     */
    public void applyPendingTransactions() {
        if (pendingTransactions.size() == 0) {
            return;
        }
        writeLock.lock();
        try {
            savedBalance.set(getStampedBalance(-1, false));
            pendingTransactions.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountNumber=" + accountNumber +
                ", savedBalance=" + savedBalance.get() +
                ", pendingTransactions=" + pendingTransactions.entrySet() +
                '}';
    }
}
