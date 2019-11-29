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

    private final long accountNumber;

    private final AtomicLong balance;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    //pending transactions to collect the transfers
    private final Map<Transfer, Integer> pendingTransactions = new ConcurrentHashMap<>();

    /**
     * constructor with args:
     * @param acntNumber account number
     * @param balance initial balance
     */
    public Account(long acntNumber, long balance) {
        this.accountNumber = acntNumber;

        this.balance = new AtomicLong(balance);
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
    public long getBalance() {
            return balance.get();
    }

    /**
     * calculate real balance as sum of saved balance and pending transfer parts
     *
     * @return calculated balance
     */
    public long getRealBalance() {
        readLock.lock();
        try {
            long savedBalance = balance.get();
            long pendingAmt = pendingTransactions.entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getKey().isProcessed())
                    .mapToLong(e -> e.getValue() * e.getKey().getAmount())
                    .sum();

            return savedBalance + pendingAmt;

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
            long oldBalance = getRealBalance();
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
            balance.set(getRealBalance());
            pendingTransactions.clear();
        } finally {
            writeLock.unlock();
        }
    }
}
