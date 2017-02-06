package trader.bot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Types.Right;

import trader.bot.domain.TAccount;
import trader.bot.domain.TContract;
import trader.bot.domain.TPosition;
import trader.bot.exceptions.NoMarketDataException;
import trader.bot.exceptions.TargetContractNotFoundException;

public class Algorithm {

	public static void selectTargetContract(TAccount account, ArrayList<ContractDetails> list, double underLyingPrice)
			throws TargetContractNotFoundException {

		double lowestPut = 10000;
		double highestPut = 0;
		double lowestCall = 10000;

		TContract putLongTarget = null;
		TContract putShortTarget = null;
		TContract callShortTarget = null;

		for (ContractDetails contractDetail : list) {
			Contract contract = contractDetail.contract();
			TContract targetContract = new TContract(contract);

			double daysDelta = targetContract.getDaysToTarget();
			double strike = contract.strike();

			if (isTargetStrike(contractDetail.contract(), underLyingPrice) && ((daysDelta == 0) || (daysDelta == -1))) {

				if (contract.right() == Right.Put) {
					if (strike < lowestPut) {
						putLongTarget = targetContract;
						lowestPut = strike;
					}

					if (contract.strike() > highestPut) {
						putShortTarget = targetContract;
						highestPut = strike;
					}

				} else {
					if (contract.strike() < lowestCall) {
						callShortTarget = targetContract;
						lowestCall = strike;
					}
				}
			}
		}

		if ((callShortTarget == null) || (putShortTarget == null) || (putLongTarget == null)) {
			throw new TargetContractNotFoundException("One or more target contracts could not be deduced.");
		} else {
			account.setCallShortTarget(callShortTarget);
			account.setPutShortTarget(putShortTarget);
			account.setPutLongTarget(putLongTarget);
		}
	}

	// Calculates the number of positions that can be opened given a specific
	// per position margin
	public static int getOpenPositionRoom(int activeSellOrders, double netLiqValue, int numOpenPositions,
			int marginPerPosition) {

		int maxPositions = (int) Math.floor(netLiqValue / marginPerPosition);
		int positionsRoom = (maxPositions - numOpenPositions) - activeSellOrders;

		return ((positionsRoom >= 0) ? positionsRoom : 0);
	}

	public static double calculateBullPutPrice(TContract putLongTarget, TContract putShortTarget)
			throws NoMarketDataException {
		double longPrice = putLongTarget.getMidPointPrice();
		double shortPrice = putShortTarget.getMidPointPrice();
		double price = shortPrice - longPrice;

		return normalizePrice(price);
	}

	public static double normalizePrice(double price) throws NoMarketDataException {
		price = price * 100;
		double residual = price % 5;

		if (residual != 0) {
			price = Math.round(price + residual);
		}

		price = price / 100;

		if (price <= 0) {
			throw new NoMarketDataException("Order price calculation lead to a value of 0.");
		}

		return price;
	}

	// Algorithm for calculating whether position needs to be closed
	public static boolean needsToBeClosed(TPosition position, double riskFactor, int lossLimit) {

		boolean needsToBeClosed = false;

		double avgValue = position.getAvgValue() * -1;
		double avgCost = position.getAvgCost();
		double avgLoss = avgValue - avgCost;

		if (avgValue > (riskFactor * avgCost)) {
			if (avgLoss < lossLimit) {
				needsToBeClosed = true;
			} else {
				Util.sendNotification("Positions are above loss limit for automatic closure.");
			}
		}

		return needsToBeClosed;
	}

	public static double targetDaysDelta(Contract contract, int daysDelta) {
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

	private static boolean isTargetStrike(Contract contract, double underLyingPrice) {

		boolean isInStrikeRange = false;
		boolean isInMoney = true;

		// Is a week from today
		double strike = contract.strike();

		// Is within the right % range
		double priceDiff = Math.abs(underLyingPrice - strike);
		double diffPercent = priceDiff / underLyingPrice;

		if (contract.right() == Right.Put) {
			isInStrikeRange = (diffPercent < .15 && diffPercent > .075);
		} else {
			isInStrikeRange = (diffPercent < .06 && diffPercent > .03);
		}

		// Is out of the money
		isInMoney = ((contract.right() == Right.Put) && (strike >= underLyingPrice))
				|| ((contract.right() == Right.Call) && (strike <= underLyingPrice));

		return (isInStrikeRange && !isInMoney);
	}



}
