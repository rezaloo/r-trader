package trader.bot.domain;

import com.ib.client.Contract;
import com.ib.controller.Position;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class TPosition {

	private Position position;

	public TPosition(Position position) {
		this.position = position;
	}

	public double getMarketPosition() {
		return position.position();
	}

	public double getAvgCost() {
		return Math.abs(Math.round(position.averageCost() * 10000d) / 10000d);
	}

	public double getMarketValue() {
		return Math.abs(Math.round(position.marketValue() * 10000d) / 10000d);
	}

	public String getDescription() {
		return position.contract().description();
	}

	public String getType() {
		return position.contract().getRight();
	}

	public double getAvgValue() {
		double value = (getMarketValue() / getNumberOfContracts());
		return (double)Math.round(value * 10000d) / 10000d;
	}

	public int getNumberOfContracts() {
		return (int) Math.round(getMarketPosition() * -1);
	}

	public Contract getContract() {
		return this.position.contract();
	}

	public ArrayList getAsObjectArray() {

		Contract contract = position.contract();

		NumberFormat dFormat = new DecimalFormat("$0.00");

		ArrayList objectArray = new ArrayList();
		objectArray.add((Double)position.position());
		objectArray.add(contract.lastTradeDateOrContractMonth());
		objectArray.add((Double)contract.strike());
		objectArray.add(contract.right());
		objectArray.add(dFormat.format(getAvgCost()));
		objectArray.add(dFormat.format(getAvgValue()));

		return objectArray;
	}
}
