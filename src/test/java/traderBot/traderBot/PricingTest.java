package traderBot.traderBot;

import com.ib.client.Contract;
import com.ib.client.TickType;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import trader.bot.Algorithm;
import trader.bot.domain.TContract;
import trader.bot.exceptions.NoMarketDataException;

/**
 * Unit test for simple App.
 */
public class PricingTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PricingTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( PricingTest.class );
    }


    // Test for naked calls pricing
    public void testNakedCallPricing() throws NoMarketDataException {

        Contract contract = new Contract();
        contract.lastTradeDateOrContractMonth("20170213");

        TContract tContract = new TContract(contract);

        // Both bid and asks exist
        tContract.tickPrice(TickType.ASK, 0.5, 1);
        tContract.tickPrice(TickType.BID, 1.0, 1);
        assertEquals(0.75, tContract.getMidPointPrice());

        // No asks, but bid exist
        tContract.tickPrice(TickType.ASK, -1.0, 1);
        tContract.tickPrice(TickType.BID, 0.35, 1);
        assertEquals(0.2, tContract.getMidPointPrice());


        // No bids, but asks exist
        tContract.tickPrice(TickType.ASK, 0.10, 1);
        tContract.tickPrice(TickType.BID, -1.0, 1);
        assertEquals(0.10, tContract.getMidPointPrice());

        // No bids nor ask
        tContract.tickPrice(TickType.ASK, -1.0, 1);
        tContract.tickPrice(TickType.BID, -1.0, 1);
        boolean noMarketDataException = false;
        try {
            assertEquals(0, tContract.getMidPointPrice());
        } catch (NoMarketDataException e) {
            noMarketDataException = true;
        }
        assertTrue(noMarketDataException);

    }

    public void testBullPutPricing() throws NoMarketDataException {

        Contract longLeg = new Contract();
        longLeg.lastTradeDateOrContractMonth("20170213");
        TContract tLongLeg = new TContract(longLeg);

        Contract shortLeg = new Contract();
        shortLeg.lastTradeDateOrContractMonth("20170213");
        TContract tShortLeg = new TContract(shortLeg);

        tLongLeg.tickPrice(TickType.ASK, -1.0, 1);
        tLongLeg.tickPrice(TickType.BID, 0.25, 1);
        tShortLeg.tickPrice(TickType.ASK, 0.25, 1);
        tShortLeg.tickPrice(TickType.BID, 0.45, 1);
        assertEquals(0.20, Algorithm.calculateBullPutPrice(tLongLeg, tShortLeg));


        tLongLeg.tickPrice(TickType.ASK, 0.05, 1);
        tLongLeg.tickPrice(TickType.BID, -1.0, 1);
        tShortLeg.tickPrice(TickType.ASK, 0.35, 1);
        tShortLeg.tickPrice(TickType.BID, 0.25, 1);
        assertEquals(0.25, Algorithm.calculateBullPutPrice(tLongLeg, tShortLeg));


        tLongLeg.tickPrice(TickType.ASK, 0.05, 1);
        tLongLeg.tickPrice(TickType.BID, -1.0, 1);
        tShortLeg.tickPrice(TickType.ASK, -1.0, 1);
        tShortLeg.tickPrice(TickType.BID, 0.25, 1);
        assertEquals(0.10, Algorithm.calculateBullPutPrice(tLongLeg, tShortLeg));

        tLongLeg.tickPrice(TickType.ASK, 0.05, 1);
        tLongLeg.tickPrice(TickType.BID, -1.0, 1);
        tShortLeg.tickPrice(TickType.ASK, 0.25, 1);
        tShortLeg.tickPrice(TickType.BID, -1.0, 1);
        assertEquals(0.20, Algorithm.calculateBullPutPrice(tLongLeg, tShortLeg));

    }
}
