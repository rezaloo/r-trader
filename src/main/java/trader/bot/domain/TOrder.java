package trader.bot.domain;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.Types.Action;
import com.ib.controller.ApiController.IOrderHandler;

public class TOrder implements IOrderHandler {

	private Contract contract = null;
	private Order order = null;
	private OrderStatus status = OrderStatus.Unknown;

	public String toString() {
		String value = "\n   - status: " + status + "\n";
		value = value + "   - price: " + order.lmtPrice();
		value = value + "   - quantity: " + order.totalQuantity();
		value = value + "   - legs: " + contract.comboLegs().size() + "\n";
		value = value + "   - contract: \n" + contract;

		return value;
	}

	public TOrder(Contract contract, Order order, OrderStatus orderStatus) {
		this.contract = contract;
		this.order = order;
		this.status = orderStatus;
	}

	public int getQuantity() {
		return (int) order.totalQuantity();
	}
	
	public int getId() {
		return this.order.orderId();
	}

	public boolean isActive() {
		 
		boolean isActive = ((status == OrderStatus.ApiPending) || (status == OrderStatus.PendingSubmit)
				|| (status == OrderStatus.PreSubmitted) || (status == OrderStatus.Submitted));

		return isActive;
	}
	
	public boolean isSellOrder() {
		return (order.action() == Action.SELL);
	}

	@Override
	public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId,
			int parentId, double lastFillPrice, int clientId, String whyHeld) {

		System.out.println("--- Order status change from : " + this.status + " to " + status);
		this.status = status;
	}

	
	// -------------------- Interface methods not being used

	@Override
	public void handle(int errorCode, String errorMsg) {
	}

	@Override
	public void orderState(OrderState orderState) {
	}
}
