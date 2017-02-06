package trader.bot.domain;

import com.ib.client.Contract;
import com.ib.controller.Position;

public class TPosition {

	private Position position;

	public String toString() {

		String value = "   - description: " + getDescription() + "\n";
		value = value + "   - type: " + getType() + "\n";
		value = value + "   - average cost: " + getAvgCost() + "\n";
		value = value + "   - market position: " + getMarketPosition() + "\n";
		value = value + "   - market value: " + getMarketValue() + "\n";
		value = value + "   - average value: " + getAvgValue() + "\n";
		value = value + "   - strike: " + getStrike() + "\n";

		return value;
	}

	public TPosition(Position position) {
		this.position = position;
	}

	public double getMarketPosition() {
		return position.position();
	}

	public double getAvgCost() {
		return (double)Math.round(position.averageCost() * 10000d) / 10000d;
	}

	public double getMarketValue() {
		return (double)Math.round(position.marketValue() * 10000d) / 10000d;
	}

	public String getDescription() {
		return position.contract().description();
	}

	public String getType() {
		return position.contract().getRight();
	}

	private double getStrike() {
		return position.contract().strike();
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
}
