package trader.bot.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import com.ib.client.ContractDetails;

import trader.bot.Broker;
import trader.bot.Util;
import trader.bot.Algorithm;
import trader.bot.exceptions.NoMarketDataException;
import trader.bot.exceptions.TargetContractNotFoundException;

public class TAccount {

	public static final String CALLS = "C";
	public static final String PUTS = "P";

	Broker connectionHandler = null;
	private String name = "";
	private int netLiqValue = 0;

	private HashMap<String, TPosition> openPositions = new HashMap<String, TPosition>();
	private HashMap<Integer, TOrder> placedOrders = new HashMap<Integer, TOrder>();

	private TContract putLongTarget = null;
	private TContract putShortTarget = null;
	private TContract callShortTarget = null;

	private double spPrice = 0;

	public TAccount() {
		connectionHandler = new Broker(this);

		updateSpPrice();
		initialize();
	}

	public double getSpPrice() {
		return spPrice;
	}

	public synchronized String toString() {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());

		String value = "\n" + timestamp + "\n";
		value = value + "Account Name: " + name + "\n";
		value = value + "Cash Balance: " + netLiqValue + "\n\n";
		value = value + "S&P 500: " + spPrice + "\n\n";

		value = value + "--- Open Positions:\n";
		for (TPosition position : openPositions.values()) {
			value = value + position;
			value = value + "    ---\n";
		}

		value = value + "\n--- Target Contracts:\n";
		value = value + "expiry,strike,type,bid,ask,close,high,low" + "\n";
		value += putLongTarget + "\n";
		value += putShortTarget + "\n";
		value += callShortTarget + "\n";

		value = value + "\n--- Placed Orders:\n";
		for (TOrder order : placedOrders.values()) {
			value = value + order;
		}
		value = value + "---\n\n";

		return value;
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

	// Open new weekly positions in a way that allows for the provided margin
	// for the new positions
	public synchronized void openPositions(int marginPerPosition, int maxDaysOut) {

		// Calculate the number of positions to open
		int callsToOpen = Algorithm.getOpenPositionRoom(getNumberOfActiveSellOrders(), netLiqValue,
				getNumberOfContracts(CALLS), marginPerPosition);
		int putsToOpen = Algorithm.getOpenPositionRoom(getNumberOfActiveSellOrders(), netLiqValue,
				getNumberOfContracts(PUTS), marginPerPosition);

		System.out.println("---\n" + "Calls to open: " + callsToOpen + "\nPuts to open : " + putsToOpen + "\n---\n");

		if (callsToOpen > 0) {
			openNakedCall(callsToOpen);
		}

		if (putsToOpen > 0) {
			openPutSpread(putsToOpen);
		}
	}

	public synchronized void closePositions(double riskFactor, int lossLimit) {

		// Monitor accounts and close them accordingly
		for (TPosition position : openPositions.values()) {
			if (Algorithm.needsToBeClosed(position, riskFactor, lossLimit)) {

				Util.sendNotification("Closing position: " + position);
				closePosition(position);
			}
		}
	}

	public synchronized String getName() {
		return name;
	}

	public synchronized void setName(String name) {
		this.name = name;
	}

	public synchronized int getNetLiqValue() {
		return netLiqValue;
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

	public synchronized void clearTargetContracts() {
		connectionHandler.unregisterForUpdates(putLongTarget);
		putLongTarget = null;

		connectionHandler.unregisterForUpdates(putShortTarget);
		putShortTarget = null;

		connectionHandler.unregisterForUpdates(callShortTarget);
		callShortTarget = null;
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

	public synchronized void closeActiveOrders() {

		// TODO: Optimization to only close orders for which the mid-point has
		// changed
		for (TOrder order : placedOrders.values()) {
			if (order.isActive()) {
				this.connectionHandler.cancelOrder(order.getId());
			}
		}
	}

	public synchronized void filterTargetContract(ArrayList<ContractDetails> list) {
		clearTargetContracts();
		try {
			Algorithm.selectTargetContract(this, list, this.spPrice);

			connectionHandler.registerForUpdates(putLongTarget);
			connectionHandler.registerForUpdates(putShortTarget);
			connectionHandler.registerForUpdates(callShortTarget);
		} catch (TargetContractNotFoundException e) {

			System.out.println("!!! - Target contracts not found.");

			putLongTarget = null;
			putShortTarget = null;
			callShortTarget = null;
		}
	}

	public void initialize() {
		this.connectionHandler.registerForUpdates();
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

	private synchronized int getNumberOfContracts(String contractType) {
		int count = 0;

		for (TPosition position : openPositions.values()) {
			// Count naked positions only
			if (position.getType().equals(contractType) && (position.getMarketValue() < 0)) {
				count = count + position.getNumberOfContracts();
			}
		}

		return ((count >= 0) ? count : 0);
	}

	private synchronized void openPutSpread(int number) {
		try {
			this.connectionHandler.placeBullPutOrder(putLongTarget, putShortTarget, number,
					Algorithm.calculateBullPutPrice(putLongTarget, putShortTarget));
		} catch (NoMarketDataException e) {
			System.out.println("!!! - New put positions could not be opened due to unavailability of market data");
		}
	}

	private synchronized void openNakedCall(int number) {
		try {
			this.connectionHandler.placeNakedSellOrder(callShortTarget, number);
		} catch (NoMarketDataException e) {
			System.out.println("!!! - New put positions could not be opened due to unavailability of market data");
		}
	}

	private void closePosition(TPosition position) {

		this.connectionHandler.placeBuyOrder(position);
	}

}
