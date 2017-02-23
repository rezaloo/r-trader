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

        if(text.equals("quote")) {
            response = getQuote();
        } else if(text.equals("target")) {
            response = getTargets();
        }


        return response.encodedMessage();
    }

    private RichMessage getTargets() {

        String response = "";

        // TODO: Parameterize margins
        int putsToOpen = account.getRoom(25000, TAccount.PUTS);
        int callsToOpen = account.getRoom(25000, TAccount.CALLS);

        response += "\n------------------------------------------------------------------\n";
        response += "S&P 500 Price is " + account.getSpPrice();
        response += "\n------------------------------------------------------------------\n";
        response += "Target last updated at: " + account.getTargetTimeStamp();
        response += "\n------------------------------------------------------------------\n";
        response += "# of Bull Puts to Open: " + putsToOpen + "\n";
        response += "Bull Put Leg 1: " + account.getPutLongTarget() + "\n";
        response += "Bull Put Leg 2: " + account.getPutShortTarget();
        response += "\n------------------------------------------------------------------\n";
        response += "# of Calls to Open: " + callsToOpen + "\n";
        response += "Call Leg 1: " + account.getCallShortTarget();
        response += "\n------------------------------------------------------------------\n";

        return buildResponse(response);
    }

    private RichMessage getQuote() {
        return buildResponse("S&P 500 Price is " + account.getSpPrice());
    }

    private RichMessage buildResponse(String message) {
        RichMessage richMessage = new RichMessage(message);
        richMessage.setResponseType("in_channel");

        return richMessage;
    }
}
