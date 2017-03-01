package trader.bot.slackbot;


import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trader.bot.domain.TAccount;


/**
 * Sample Slash Command Handler.
 *
 * @author ramswaroop
 * @version 1.0.0, 20/06/2016
 */
@RestController
public class RTypeCommand {

    private static final Logger logger = LoggerFactory.getLogger(RTypeCommand.class);

    /**
     * The token you get while creating a new Slash Command. You
     * should paste the token in application.properties file.
     */
    @Value("${slashCommandToken}")
    private String slackToken;

    private TAccount account = TAccount.getInstance();

    /**
     * Slash Command handler. When a user types for example "/app help"
     * then slack sends a POST request to this endpoint. So, this endpoint
     * should match the url you set while creating the Slack Slash Command.
     *
     * @param token
     * @param teamId
     * @param teamDomain
     * @param channelId
     * @param channelName
     * @param userId
     * @param userName
     * @param command
     * @param text
     * @param responseUrl
     * @return
     */
    @RequestMapping(value = "/r-type",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommand(@RequestParam("token") String token,
                                             @RequestParam("team_id") String teamId,
                                             @RequestParam("team_domain") String teamDomain,
                                             @RequestParam("channel_id") String channelId,
                                             @RequestParam("channel_name") String channelName,
                                             @RequestParam("user_id") String userId,
                                             @RequestParam("user_name") String userName,
                                             @RequestParam("command") String command,
                                             @RequestParam("text") String text,
                                             @RequestParam("response_url") String responseUrl) {
        // validate token
        if (!token.equals(slackToken)) {
            return new RichMessage("Sorry! You're not lucky enough to use our slack command.");
        }

        RichMessage response = new RichMessage("Sorry, don't understand what you are looking for.");;

        if(text.equals("quote") || text.equals("q")) {
            response = getQuote(account);
        } else if(text.equals("contracts") || text.equals("c")) {
            response = getContracts(account);
        } else if(text.equals("positions") || text.equals("p")) {
            response = getPositions(account);
        } else if(text.equals("orders") || text.equals("o")) {
            response = getOrders(account);
        }

        return response.encodedMessage();
    }

    public static RichMessage getPositions(TAccount tAccount) {

        String response = tAccount.positionsToString();
        return buildResponse(response);
    }

    public static RichMessage getContracts(TAccount tAccount) {

        String response = tAccount.contractsToString() + "\n";

        // TODO: Parameterize margins
        int putsToOpen = tAccount.getRoom(25000, TAccount.PUTS);
        int callsToOpen = tAccount.getRoom(25000, TAccount.CALLS);

        response += "Room for BP: " + putsToOpen + "\n";
        response += "Room for C : " + callsToOpen + "\n";

        return buildResponse(response);
    }

    public static RichMessage getOrders(TAccount tAccount) {

        String response = tAccount.ordersToString();
        return buildResponse(response);
    }

    public static RichMessage getQuote(TAccount tAccount) {
        return buildResponse("S&P 500 Price is " + tAccount.getSpPrice());
    }

    public static RichMessage buildResponse(String message) {

        message = "```" + message + "```\n";

        RichMessage richMessage = new RichMessage(message);
        richMessage.setResponseType("in_channel");

        return richMessage;
    }
}
