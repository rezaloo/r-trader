package trader.bot.domain;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import com.ib.client.ContractDetails;

import dnl.utils.text.table.TextTable;
import trader.bot.Broker;
import trader.bot.Util;
import trader.bot.Algorithm;
import trader.bot.exceptions.NoMarketDataException;
import trader.bot.exceptions.TargetContractNotFoundException;

public class TAccount {

    public static final String CALLS = "C";
    public static final String PUTS = "P";

    private static TAccount instance = null;

    private Broker connectionHandler = null;
    private String name = "";
    private int netLiqValue = 0;

    private HashMap<String, TPosition> openPositions = new HashMap<String, TPosition>();
    private HashMap<Integer, TOrder> placedOrders = new HashMap<Integer, TOrder>();
    private ArrayList<TContract> contracts = new ArrayList<TContract>();

    private TContract putLongTarget = null;
    private TContract putShortTarget = null;
    private TContract callShortTarget = null;

    private Timestamp targetTimeStamp = null;

    private double spPrice = 0;

    private TAccount() {
        connectionHandler = new Broker(this);
        connectionHandler.registerForUpdates();
        updateSpPrice();
    }

    public static TAccount getInstance() {
        if (instance == null) {
            instance = new TAccount();
        }

        return instance;

    }

    public synchronized String toString() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        String value = "\n" + timestamp + "\n";
        value = value + "Account Name: " + name + "\n";
        value = value + "Cash Balance: " + netLiqValue + "\n\n";
        value = value + "S&P 500: " + spPrice + "\n\n";

        value = value + "--- Open Positions\n";
        value = value + positionsToString();
        value = value + "\n--- Target Contracts\n";
        value = value + contractsToString();
        value = value + "\n--- Orders\n";
        value = value + ordersToString();

        return value;
    }

    public double updateSpPrice() {
        try {
            spPrice = Util.getUnderLyingPrice();
        } catch (NoMarketDataException e) {
            System.out.println("!!! Could not get price of SPX.");
            spPrice = 0;
        }

        return spPrice;
    }

    public synchronized String positionsToString() {

        String[] columnNames = {"#", "EXP", "STRK", "TYPE", "PREM", "VAL", "OOM"};
        ArrayList<ArrayList> dataAL = new ArrayList<>();

        for (TPosition position : openPositions.values()) {
            ArrayList objectArray = position.getAsObjectArray();
            objectArray.add(Algorithm.oom(position.getContract().strike(), spPrice));
            dataAL.add(objectArray);
        }

        Object[][] data = new Object[dataAL.size()][];
        int i = 0;
        for (ArrayList objectArray : dataAL) {
            data[i] = objectArray.toArray();
            i++;
        }

        TextTable textTable = new TextTable(columnNames, data);
        textTable.setSort(2);

        return generateString(textTable);
    }

    public synchronized String contractsToString() {

        String value = "Contracts not initialized.";

        if (isInitialized()) {

            String[] columnNames = {"EXP", "STRK", "TYPE", "BID", "ASK", "OOM"};
            Object[][] data = new Object[3][];

            data[0] = toObjectArray(putLongTarget);
            data[1] = toObjectArray(putShortTarget);
            data[2] = toObjectArray(callShortTarget);

            TextTable textTable = new TextTable(columnNames, data);
            textTable.setSort(2);
            return generateString(textTable);
        }

        return value;
    }

    private Object[] toObjectArray(TContract contract) {

        return(new Object[]{contract.getContract().lastTradeDateOrContractMonth(),
                (Double)contract.getContract().strike(),
                contract.getContract().right(),
                (Double)contract.getBid(),
                (Double)contract.getAsk(),
                Algorithm.oom(contract.getContract().strike(), spPrice)});
    }

    public synchronized String ordersToString() {

        // TODO: Convert to table format

        String value = "\n--- Placed Orders\n";
        for (TOrder order : placedOrders.values()) {
            value = value + order;
        }
        value = value + "---\n\n";

        return value;
    }

    public double getSpPrice() {
        return spPrice;
    }

    public synchronized void setCallShortTarget(TContract callShortTarget) {
        this.callShortTarget = callShortTarget;
    }

    public synchronized void setPutShortTarget(TContract putShortTarget) {
        this.putShortTarget = putShortTarget;
    }

    public synchronized void setPutLongTarget(TContract putLongTarget) {
        this.putLongTarget = putLongTarget;
    }

    public synchronized void addPosition(TPosition position) {
        if (position.getAvgCost() > 0) {
            openPositions.put(position.getDescription(), position);
        }
    }

    public int getRoom(int marginPerPosition, String type) {
        return Algorithm.getOpenPositionRoom(getNumberOfActiveSellOrders(), netLiqValue,
                getNumberOfOpenPositions(type), marginPerPosition);
    }

    public synchronized void checkPositions(double riskFactor, int lossLimit) {

        // Monitor accounts and close them accordingly
        for (TPosition position : openPositions.values()) {
            if (Algorithm.needsToBeClosed(position, riskFactor, lossLimit)) {

                System.out.println("--- " + "Closing position: " + position);
            }
        }
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized void setNetLiqValue(int cashBalance) {
        this.netLiqValue = cashBalance;
    }

    public synchronized boolean isInitialized() {
        boolean targeCallsSet = (this.callShortTarget != null) && (this.putLongTarget != null)
                && (this.putShortTarget != null);
        boolean underlyingSet = (spPrice != 0);

        return (targeCallsSet && underlyingSet);
    }



    public synchronized void addPlacedOrder(TOrder placedOrder) {
        placedOrders.put(placedOrder.getId(), placedOrder);
    }

    public synchronized void removePlacedOrder(int orderId) {
        placedOrders.remove(orderId);
    }

    public synchronized int getNumberOfActiveSellOrders() {
        int number = 0;
        for (TOrder order : placedOrders.values()) {
            if (order.isActive() && order.isSellOrder()) {
                number += order.getQuantity();
            }
        }

        return number;
    }

    public synchronized void processTargetContracts(ArrayList<ContractDetails> list) {
        clearTargetContracts();
        setTargetContracts(list);
        filterContracts();

        targetTimeStamp = new Timestamp(System.currentTimeMillis());
    }

    public synchronized void filterContracts() {
        try {
            Algorithm.selectTargetContract(this, this.contracts, this.spPrice);
        } catch (TargetContractNotFoundException e) {

            System.out.println("!!! - Target contracts not found.");

            putLongTarget = null;
            putShortTarget = null;
            callShortTarget = null;
        }
    }

    private synchronized void clearTargetContracts() {

        for(TContract contract : contracts) {
            connectionHandler.unregisterForUpdates(contract);
        }

        contracts.clear();

        putLongTarget = null;
        putShortTarget = null;
        callShortTarget = null;
    }

    private synchronized void setTargetContracts(ArrayList<ContractDetails> list) {

        for(ContractDetails cd : list) {

            if(tc.ExpiryIsSoonerThan("Today + 1 week")) {
                TContract tc = new TContract(cd.contract());
                connectionHandler.registerForUpdates(tc);
                contracts.add(tc);
            }
        }
    }

    private synchronized int getNumberOfOpenPositions(String contractType) {
        int count = 0;

        for (TPosition position : openPositions.values()) {
            // Count naked positions only
            if (position.getType().equals(contractType)
                    && (position.getMarketPosition() < 0)) {
                count = count + position.getNumberOfContracts();
            }
        }

        return ((count >= 0) ? count : 0);
    }

    private String generateString(TextTable textTable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        textTable.printTable(ps, 1);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

}
