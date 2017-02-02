package trader.bot;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import trader.bot.exceptions.NoMarketDataException;

public class Util {


	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

	public static double getUnderLyingPrice() throws NoMarketDataException {

		double value = 0;

		try {
			String response = null;

			response = Unirest.get("http://finance.google.com/finance/info?client=ig&q=.INX")
					.header("authorization", "WpbO0IWzDWSLxgrL1Utg9WOxmmrttep4B6wqqISg")
					.header("cache-control", "no-cache").header("postman-token", "e90ef285-d1f8-2f1e-fe42-79bef6e6e23b")
					.asString().getBody();

			response = response.substring(5);
			response = response.replace(']', ' ');

			JsonNode jsonResponse = new JsonNode(response);
			String stringValue = jsonResponse.getObject().getString("l_cur");
			stringValue = stringValue.replaceAll(",", "");
			value = Double.parseDouble(stringValue.trim());

		} catch (Exception e) {
			throw new NoMarketDataException("Could not get price of INX.");
		}

		return value;
	}

	public static void sendNotification(String message) {

		try {
			System.out.println("--- " + message);
			
			Unirest.post("https://api.sendgrid.com/api/mail.send.json")
					.header("content-type", "multipart/form-data; boundary=---011000010111000001101001")
					.header("authorization", "WpbO0IWzDWSLxgrL1Utg9WOxmmrttep4B6wqqISg")
					.header("cache-control", "no-cache").header("postman-token", "4baf993a-c7be-f1cb-9e2d-87db6d9e7344")
					.body("-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"api_user\"\r\n\r\nrezaloo\r\n-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"api_key\"\r\n\r\nbalmoral1\r\n-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"to\"\r\n\r\nshafii.reza@gmail.com\r\n-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"from\"\r\n\r\nshafii.reza@gmail.com\r\n-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"subject\"\r\n\r\nTB Alert!\r\n-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"text\"\r\n\r\n"
							+ message + "\r\n-----011000010111000001101001--")
					.asString();
		} catch (UnirestException e) {
			throw new RuntimeException();
		}
	}
}
