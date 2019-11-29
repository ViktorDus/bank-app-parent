package ru.vdusanyuk.bank;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.vdusanyuk.bank.api.ServiceResponse;
import ru.vdusanyuk.bank.api.TransferRequest;
import ru.vdusanyuk.bank.dao.BankHolder;
import ru.vdusanyuk.bank.rest.EntryPoint;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * test cases based on Jersy servlet + Jetty container
 */

public class BankEntryPointTest extends JerseyTest {
    private final static Logger logger = Logger.getLogger(BankEntryPointTest.class);
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
        //need some time for all threads processed and stopped
        Thread.sleep(3000);
        super.tearDown();
        logger.info("End test");
    }

    @Test
    public void testBalance() {
        checkTotalBalance();
    }

    @Test
    public void testAccountRequestById() {
        logger.info("Strart testGetAccountById");
        ServiceResponse serviceResponse = doAccountRequest(5L);
        assertEquals("Should return responseStatus SUCCESS", "SUCCESS", serviceResponse.getResponseStatus());
        logger.info(serviceResponse);
    }

    @Test
    public void testAccountRequestByNoneExistingId() {
        logger.info("Strart testGetAccountByNoneExistingId");
        ServiceResponse serviceResponse = doAccountRequest(11L);
        assertEquals("Should return responseStatus ERROR", "ERROR", serviceResponse.getResponseStatus());
        logger.info(serviceResponse);
    }

    @Test
    public void testSingleTransfer() throws Exception {
        logger.info("Strart testSingleTransfer");
        ServiceResponse serviceResponse = doTrasferRequest(3L, 9L, 33L);
        assertEquals("Should return responseStatus SUCCESS", "SUCCESS", serviceResponse.getResponseStatus());
        //check if deposit happened
        serviceResponse = doAccountRequest(9L);
        assertEquals("Should return responseStatus SUCCESS", "SUCCESS", serviceResponse.getResponseStatus());
        assertEquals(133L, (long) serviceResponse.getBalance());
        //check if withdraw happened
        serviceResponse = doAccountRequest(3);
        assertEquals("Should return responseStatus SUCCESS", "SUCCESS", serviceResponse.getResponseStatus());
        assertEquals(67L, (long) serviceResponse.getBalance());
        checkTotalBalance();
        //wait for asynchronos processing the pending transactions
        Thread.sleep(1000);
        //additional check after pending queue processed
        serviceResponse = doAccountRequest(9L);
        assertEquals("Should return responseStatus SUCCESS", "SUCCESS", serviceResponse.getResponseStatus());
        assertEquals(133L, (long) serviceResponse.getBalance());
        checkTotalBalance();
    }

    @Test
    public void testTransferNegativeAmount() throws Exception {
        logger.info("Strart testTransferNegativeAmount");
        ServiceResponse serviceResponse = doTrasferRequest(4, 8, -20);
        assertEquals("Should return responseStatus ERROR", "ERROR", serviceResponse.getResponseStatus());
        logger.info(serviceResponse);
    }

    @Test
    public void testTransferExceedBalance() throws Exception {
        logger.info("Strart testTransferExceedBalance");
        ServiceResponse serviceResponse = doTrasferRequest(3, 9, 45);
        assertEquals("Should return responseStatus SUCCESS", "SUCCESS", serviceResponse.getResponseStatus());
        serviceResponse = doTrasferRequest(3, 9, 60);
        assertEquals("Should return responseStatus ERROR", "ERROR", serviceResponse.getResponseStatus());
        logger.info(serviceResponse);
        //make sure balance id not changed
        checkTotalBalance();
    }

    private ServiceResponse doTrasferRequest(long fromAccountNumber, long toAccountNumber, long amount) throws Exception {
        TransferRequest transfer = new TransferRequest(fromAccountNumber, toAccountNumber, amount);
        Response output = target(TRANSFER_MONEY_PATH).request().post(Entity.entity(transfer, MediaType.APPLICATION_JSON));
        if (200 == output.getStatus()) {
            //giving small delay to response
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

    private ServiceResponse doAccountRequest(long AccountNumber) {
        Response output = target("/bankService/account/" + AccountNumber).request().get();
        assertEquals("Should return status 200", 200, output.getStatus());
        return output.readEntity(ServiceResponse.class);
    }

}
