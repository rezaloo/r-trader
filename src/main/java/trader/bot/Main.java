package trader.bot;

import trader.bot.domain.TAccount;

public class Main {
	public static void main(String[] args) {

		// TODO: Move parameters below into general configuration class
		// TODO: Use log4J
		// TODO: Read-only mode

		try {
			// Wait until acount is initialized
			TAccount account = new TAccount();

			while (true) {
			
				System.out.println(account);
				Thread.sleep(60000);

				if (account.isInitialized()) {

					// Close active orders so that they can be adjusted
					account.closeActiveOrders();

					// Open new weekly positions as margin permits
					account.openPositions(25000, 7);

					// Close any positions with a loss of more than 200%, unless
					// the loss is above $1000
					account.closePositions(3, 1000);
				} 
			}

		} catch (Exception e) {
			e.printStackTrace();
			Util.sendNotification("Unexpected application exception: " + e + "\n\n Exiting immediately.");
		}

	}
}
