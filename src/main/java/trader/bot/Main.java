package trader.bot;

import trader.bot.domain.TAccount;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"me.ramswaroop.jbot", "trader.bot"})
public class Main {
	public static void main(String[] args) {

		// TODO: Move parameters below into general configuration class
		// TODO: Use log4J
		// TODO: Replace notifications with Both calls

		SpringApplication.run(Main.class, args);

		try {

			TAccount account = TAccount.getInstance();

			while (true) {

				System.out.println(account);

				Thread.sleep(10000);

				account.updateSpPrice();
				account.checkPositions(4, 1000);
			}

		} catch (Exception e) {
			e.printStackTrace();
			Util.sendNotification("Unexpected application exception: " + e + "\n\n Exiting immediately.");
		}

	}
}
