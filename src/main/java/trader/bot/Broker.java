package trader.bot;

import java.util.ArrayList;
import com.ib.client.ComboLeg;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.SecType;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController.IAccountHandler;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.IContractDetailsHandler;
import com.ib.controller.ApiController.ILiveOrderHandler;

import trader.bot.domain.TAccount;
import trader.bot.domain.TContract;
import trader.bot.domain.TOrder;
import trader.bot.domain.TPosition;
import trader.bot.exceptions.NoMarketDataException;

import com.ib.controller.Position;

public class Broker implements IConnectionHandler, IAccountHandler, IContractDetailsHandler, ILiveOrderHandler {

	private ApiController ibController = null;
	private final Logger m_inLogger = new Logger();
	private final Logger m_outLogger = new Logger();

	private TAccount parentAccount = null;

	public Broker(TAccount account) {
		parentAccount = account;
		connect();
	}

	public void registerForUpdates(TContract targetContract) {
		ibController.reqOptionMktData(targetContract.getContract(), "", false, targetContract);
	}

	public void unregisterForUpdates(TContract targetContract) {
		ibController.cancelOptionMktData(targetContract);
	}
	
	public void registerForUpdates() {
		// Register for account updates
		ibController.reqAccountUpdates(true, parentAccount.getName(), this);

		// Register for SPX contract updates
		requestContractDetails();

		// Register for order updates
		ibController.reqLiveOrders(this);
	}

	@Override
	public void contractDetails(ArrayList<ContractDetails> list) {
		parentAccount.processTargetContracts(list);
	}

	@Override
	public void updatePortfolio(Position position) {
		this.parentAccount.addPosition(new TPosition(position));
	}

	@Override
	public void connected() {
		System.out.println("--- Connected.\n");
	}

	@Override
	public void accountList(ArrayList<String> list) {
		parentAccount.setName(list.get(0));
	}

	@Override
	public void disconnected() {
		System.out.println("!!! Disconnected.\n");
	}

	@Override
	public void accountValue(String account, String key, String value, String currency) {
		if (key.equals("NetLiquidationByCurrency") && currency.equals("BASE")) {
			this.parentAccount.setNetLiqValue(Integer.valueOf(value));
		}
	}

	@Override
	public void openOrder(Contract contract, Order order, OrderState orderState) {
		TOrder placedOrder = new TOrder(contract, order, orderState.status());
		parentAccount.addPlacedOrder(placedOrder);
	}

	@Override
	public void orderStatus(int orderId, OrderStatus status, double filled, double remaining, double avgFillPrice,
			long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
		if ((status == OrderStatus.ApiCancelled) || (status == OrderStatus.Cancelled)) {
			parentAccount.removePlacedOrder(orderId);
		}
	}
	
	private void connect() {
		getController().connect("127.0.0.1", 7496, 0, null);
	}

	public void disconnect() {
		getController().disconnect();
	}

	private void requestContractDetails() {
		Contract optContract = new Contract();
		optContract.symbol("SPX");
		optContract.currency("USD");
		optContract.exchange("SMART");
		optContract.secType(SecType.OPT);
		ibController.reqContractDetails(optContract, this);
	}

	private ApiController getController() {
		if (ibController == null) {
			ibController = new ApiController(this, getInLogger(), getOutLogger());
		}
		return ibController;
	}

	private ILogger getInLogger() {
		return m_inLogger;
	}

	private ILogger getOutLogger() {
		return m_outLogger;
	}

	private static class Logger implements ILogger {

		@Override
		public void log(final String str) {
			if (!str.trim().isEmpty()) {
				// System.out.println(">>> " + str);
			}
		}
	}

	// -------------------- Interface methods not being used

	@Override
	public void openOrderEnd() {
	}

	@Override
	public void handle(int orderId, int errorCode, String errorMsg) {
	}

	@Override
	public void accountTime(String timeStamp) {
	}

	@Override
	public void accountDownloadEnd(String account) {
	}

	@Override
	public void error(Exception e) {
	}

	@Override
	public void message(int id, int errorCode, String errorMsg) {
	}

	@Override
	public void show(String string) {
	}
}
