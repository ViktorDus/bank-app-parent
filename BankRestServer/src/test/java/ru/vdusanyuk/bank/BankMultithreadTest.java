package ru.vdusanyuk.bank;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.vdusanyuk.bank.json.ServiceResponse;
import ru.vdusanyuk.bank.json.TransferRequest;
import ru.vdusanyuk.bank.dao.BankHolder;
import ru.vdusanyuk.bank.rest.EntryPoint;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;


/**
 * test cases based on Jersy servlet + Jetty container
 */

public class BankMultithreadTest extends JerseyTest {
    private final static Logger logger = Logger.getLogger(BankMultithreadTest.class);
    private static String TRANSFER_MONEY_PATH = "/bankService/transfer";

    @Override
    public Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(EntryPoint.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        BankHolder.getInstance().initBankAccounts();
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(5000);
        super.tearDown();
        logger.info("End test");
    }

    @Test
    public void testMultithreadTransfersLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int j=0; j < 20; j++){
            for (int k=0; k < 5; k++){
                doAccountRequest(getRndAccountNum(0));
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

    private ServiceResponse doTrasferRequest(long fromAccountNumber, long toAccountNumber, long amount) throws Exception {
        TransferRequest transfer = new TransferRequest(fromAccountNumber, toAccountNumber, amount);
        Response output = target(TRANSFER_MONEY_PATH).request().post(Entity.entity(transfer, MediaType.APPLICATION_JSON));
        if (200 == output.getStatus()) {
            Thread.sleep(5);
            return output.readEntity(ServiceResponse.class);
        }
        throw new Exception("Service HTTP responded with status: " + output.getStatus());
    }

    private void checkTotalBalance()  {
        Response output = target("/bankService/total").request().get();
        assertEquals("should return status 200", 200, output.getStatus());
        String responseBalance = output.readEntity(String.class);
        assertEquals("Should return  total balance 1000", "1000", responseBalance);
    }

    private void doAccountRequest(long AccountNumber) {
        Response output = target("/bankService/account/"+AccountNumber).request().get();
        assertEquals("Should return status 200", 200, output.getStatus());
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
            System.out.println("attempt to shutdown executor");
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
