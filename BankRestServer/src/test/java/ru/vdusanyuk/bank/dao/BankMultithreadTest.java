package ru.vdusanyuk.bank.dao;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.vdusanyuk.bank.dao.model.OperationResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * test cases based on Jersy servlet + Jetty container
 */

public class BankMultithreadTest  {
    private final static Logger logger = Logger.getLogger(BankMultithreadTest.class);
    private static String TRANSFER_MONEY_PATH = "/bankService/transfer";

    private BankHolder bankHolder;

    @Before
    public void setUp() {
        bankHolder = BankHolder.getInstance();
        bankHolder.initBankAccounts();
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(300);
        logger.info("End test");
    }

    @Test
    public void testMultithreadTransfersLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int j=0; j < 20; j++){
            for (int k=0; k < 5; k++){
                executor.submit(() ->doAccountRequest(getRndAccountNum(0)));
                for (int m=0; m < 10; m++){
                    int fromAddrNum = getRndAccountNum(0);
                    executor.submit(() -> doTrasferRequest(fromAddrNum, getRndAccountNum(fromAddrNum), getRND50()) );
                }
            }
            executor.submit(this::checkTotalBalance);
        }
        //need enough time for completion
        Thread.sleep(5000);
        stop(executor);
        checkTotalBalance();
    }

    private OperationResult doTrasferRequest(long fromAccountNumber, long toAccountNumber, long amount) throws Exception {

        OperationResult output = bankHolder.submitTransfer(fromAccountNumber, toAccountNumber, amount);
        assertNotNull(output);
        assertEquals("Should return code 0", 0, output.getCode());
        assertEquals(Long.valueOf(fromAccountNumber), output.getAccountNumber());
        Thread.sleep(5);
        return output;
    }

    private void checkTotalBalance()  {
        Long total = bankHolder.getTotalBalance();
        assertNotNull(total);
        assertEquals("Should return  total balance 1000", 1000L, total.longValue());
    }

    private void doAccountRequest(long accountNumber) {
        OperationResult output = bankHolder.getAccount(accountNumber);
        assertNotNull(output);
        assertEquals("Should return status SUCCESS=0", 0, output.getCode());
        assertEquals(Long.valueOf(accountNumber), output.getAccountNumber());
        assertNotNull(output.getBalance());
    }

    //generate random amounts in range 1...50
    private long getRND50() {
        return (int)(Math.random() * 50) + 1;
    }

    //generate random account number 1...10
    private int getRndAccountNum(int exclude) {
        int result = exclude;
        while (result == exclude){
            result = (int) (Math.random() * 10) + 1;
        }
        return result;
    }

    private static void stop(ExecutorService executor) {
        try {
            logger.info("attempt to shutdown executor");
            executor.shutdown();
            executor.awaitTermination(20, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            logger.info("termination interrupted");
        }
        finally {
            if (!executor.isTerminated()) {
                logger.info("killing non-finished tasks");
            }
            executor.shutdownNow();
            logger.info("shutdown finished");
        }
    }

}
