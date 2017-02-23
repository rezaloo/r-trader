package trader.bot;

import trader.bot.domain.TAccount;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"me.ramswaroop.jbot", "trader.bot"})
public class Main {
	public static void main(String[] args) {

		// TODO: Move parameters below into general configuration class
		// TODO: Use log4J
		// TODO: Read-only mode

		SpringApplication.run(Main.class, args);

		try {
			// Wait until acount is initialized
			TAccount account = TAccount.getInstance();

			while (true) {

				System.out.println(account);
				Thread.sleep(60000);

				if (account.isInitialized() && (account.updateSpPrice() !=0)) {

					// Close active orders so that they can be adjusted
					account.closeFilledAndActiveOrders();

					// Open new weekly positions as margin permits
					account.openPositions(25000, 7);

					// Close positions that are in trouble
					account.closePositions(4, 1000);
				} 
			}

		} catch (Exception e) {
			e.printStackTrace();
			Util.sendNotification("Unexpected application exception: " + e + "\n\n Exiting immediately.");
		}

	}
}
