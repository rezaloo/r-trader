package trader.bot.domain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;
import com.ib.client.TickType;
import com.ib.client.Types.MktDataType;
import com.ib.controller.ApiController.IOptHandler;

import trader.bot.Algorithm;
import trader.bot.Util;
import trader.bot.exceptions.NoMarketDataException;

public class TContract implements IOptHandler {

	private Contract contract;
	private double daysToTarget;

	private double bid = 0;
	private double ask = 0;
	private double close = 0;
	private double high = 0;
	private double low = 0;

	public TContract(Contract contract) {
		this.contract = contract;
		this.daysToTarget = targetDaysDelta(contract, 7);
	}

	public String toString() {
		String value = contract.lastTradeDateOrContractMonth();
		value = value + "," + contract.strike();
		value = value + "," + contract.right();
		value = value + "," + bid + "," + ask + "," + close + "," + high + "," + low;
		return value;
	}

	public double getDaysToTarget() {
		return daysToTarget;
	}

	public double getMidPointPrice() throws NoMarketDataException {
		double price = 0;

		if (bid >= 0 && ask >= 0) {
			price = ((bid + ask) / 2);
		} 
		
//		else if (high >= 0) {
//			price = high;
//		}

		return Algorithm.normalizePrice(price);
	}

	public Contract getContract() {
		return this.contract;
	}

	@Override
	public void tickPrice(TickType tickType, double price, int canAutoExecute) {

		switch (tickType) {
		case BID:
			bid = price;
			break;
		case ASK:
			ask = price;
			break;
		case CLOSE:
			close = price;
		case HIGH:
			high = price;
		case LOW:
			low = price;
		default:
			break;
		}
	}

	private static double targetDaysDelta(Contract contract, int daysDelta) {
		String expiryDateString = contract.lastTradeDateOrContractMonth();

		Date weekFromNow = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(weekFromNow);
		calendar.add(Calendar.DATE, daysDelta);
		weekFromNow = calendar.getTime();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		Date expiryDate;

		try {
			expiryDate = formatter.parse(expiryDateString);
		} catch (ParseException e) {
			throw new RuntimeException();
		}

		long delta = Util.getDateDiff(weekFromNow, expiryDate, TimeUnit.DAYS);

		return delta;
	}

	// -------------------- Interface methods not being used

	@Override
	public void tickSize(TickType tickType, int size) {

	}

	@Override
	public void tickString(TickType tickType, String value) {
	}

	@Override
	public void tickSnapshotEnd() {
	}

	@Override
	public void marketDataType(MktDataType marketDataType) {
	}

	@Override
	public void tickOptionComputation(TickType tickType, double impliedVol, double delta, double optPrice,
			double pvDividend, double gamma, double vega, double theta, double undPrice) {
	}

}
