package ru.vdusanyuk.bank.dao;

import ru.vdusanyuk.bank.dao.model.Account;
import ru.vdusanyuk.bank.dao.model.OperationResult;
import ru.vdusanyuk.bank.dao.model.Transfer;
import ru.vdusanyuk.bank.dao.model.TransferStatus;
import ru.vdusanyuk.bank.util.AsyncBatchExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Bank Holder  holds accounts and perform the following operations:
 * - get balance,
 * - read account state,
 * - transfer money from one account to another one
 * @author vdusanyuk
 */

public class BankHolder {

    private static final int MAX_ACCOUNT_NO = 10;
    private static final int MIN_ACCOUNT_NO = 1;
    private static final int INITIAL_BALANCE = 100;

    private final static Logger logger = LoggerFactory.getLogger(BankHolder.class);

    /**
     * the single instance of BankHolder
     */
    private static BankHolder instance = new BankHolder();

    /**
     * transfer executor is used for asynchronously apply the delayed transfers to accounts
     */
    private final AsyncBatchExecutor<Transfer> transferAsyncExecutor;

    /**
     * in-memory bank accounts
     */
    private final Map<Long, Account> bankAccounts = new HashMap<>();
    /**
     * the lock is for keeping consistency of bank getTotal and batch write operation
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * private constructor for singleton, initialize members accounts
     */
    private BankHolder() {
        initBankAccounts();
        transferAsyncExecutor = new AsyncBatchExecutor<>(this::processTransfers);
    }

    /**
     * instantiate and return the singleton
     * @return bank holder instance
     */
    public static BankHolder getInstance() {
        if (instance == null) {

            instance = new BankHolder();
        }
       return instance;
    }

    /** submit transfer amount of money from source account to destination account
     *
     * @param fromAcntNumber source account number
     * @param toAcntNumber destination account number
     * @param amount amount for transfer
     * @return  result of operation {@link OperationResult}
     */
    public OperationResult submitTransfer(Long fromAcntNumber, Long toAcntNumber, long amount) {
        //generate transaction ID
        long transactionId = System.currentTimeMillis();
        logger.debug("Transfer request: trx#{}  from={}, to={}, amount={}", transactionId, fromAcntNumber, toAcntNumber, amount);
        //get accounts and validate
        Account fromAccount = bankAccounts.get(fromAcntNumber);
        Account toAccount = bankAccounts.get(toAcntNumber);
        //check existance of all the accounts and positive amount
        if (fromAccount == null || toAccount == null || amount <= 0) {
            logger.error("transfer request invalid!");
            return new OperationResult(1,"transfer request invalid!", fromAcntNumber, null);
        }

        Transfer transfer = new Transfer(transactionId, fromAccount, toAccount, amount);
        OperationResult operationResult;
        long startTime = System.currentTimeMillis();
        //we should not intersect with writing process, so need read lock
        readLock.lock();
        try {
            operationResult = fromAccount.addPendingTransaction(transfer, true);
            if (operationResult.getCode() == 0) {
                toAccount.addPendingTransaction(transfer, false);
                transfer.setStatus(TransferStatus.PENDING);
                //process transfer asynchronously
                transferAsyncExecutor.addProcessingItem(transfer);
            } else {
                logger.debug("Deposit operation is skipped because of error at withdraw");
            }
        } finally {
            readLock.unlock();
        }
        logger.info("transfer#{} {}: from={}, to={}, amount={}, new balance={}, elapsed {} ms",
                                transactionId, operationResult.getCode() == 0 ? "SUBMITTED" : "REJECTED",
                                fromAcntNumber, toAcntNumber, amount, operationResult.getBalance(),
                                System.currentTimeMillis() - startTime);
        return operationResult;
    }

    /**
     * requesting total bank balance, mostly for test purpose
     * @return total value
     */
    public Long getTotalBalance() {
        Long totalBalance;
        long startTime = System.currentTimeMillis();
        readLock.lock();
        try {
            totalBalance = bankAccounts.entrySet().parallelStream()
                     .mapToLong(acnt -> acnt.getValue().getRealBalance())
                     .sum();
        } finally {
           readLock.unlock();
        }
        logger.info("Total Balance requested, result = {}, elapsed {} ms",
                                      totalBalance, System.currentTimeMillis() - startTime);
        return totalBalance;
    }

    /**
     * request  account by account number
     *
     * @param accountNumber account number requested
     * @return operation result object
     */
    public OperationResult getAccount(Long accountNumber) {
        Account account = bankAccounts.get(accountNumber);
        readLock.lock();
        try {
            return account != null ?
                    new OperationResult(0, null, account.getAccountNumber(), account.getRealBalance()) :
                    new OperationResult(1, "NOT Found", accountNumber, null);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * processing the pending transfers by asynchronous executor
     *
     * @param transfers collection of delayed transfers being processed
     */
    private void processTransfers(Collection<Transfer> transfers) {
        if (transfers == null || transfers.isEmpty()) {
            return;
        }
        logger.debug("Async process of chunk transfers - start, size={}", transfers.size());
        long startTime = System.currentTimeMillis();
        writeLock.lock();
        try {
            transfers.parallelStream().forEach(t -> {
                t.getFromAccount().applyPendingTransactions();
                t.getToAccount().applyPendingTransactions();
                t.setStatus(TransferStatus.PROCESSED);
            });
        } finally {
            writeLock.unlock();
        }
        logger.info("Async process of chunk transfers - end, size={}, elapsed {} ms",
                    transfers.size(), System.currentTimeMillis() - startTime);
    }

    // initialize bank accounts as: 10 accounts with initial amount of 100 bitcoins
    public void initBankAccounts() {
        bankAccounts.clear();
        bankAccounts.putAll(
                LongStream.range(MIN_ACCOUNT_NO, MIN_ACCOUNT_NO + MAX_ACCOUNT_NO).boxed()
                .map(k -> new Account(k, INITIAL_BALANCE))
                .collect(Collectors.toMap(Account::getAccountNumber, acnt -> acnt))
        );
    }

}
