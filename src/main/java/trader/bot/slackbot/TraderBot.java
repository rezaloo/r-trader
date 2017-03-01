package trader.bot.slackbot;

import me.ramswaroop.jbot.core.slack.Bot;
import me.ramswaroop.jbot.core.slack.Controller;
import me.ramswaroop.jbot.core.slack.EventType;
import me.ramswaroop.jbot.core.slack.models.Event;
import me.ramswaroop.jbot.core.slack.models.Message;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import trader.bot.domain.TAccount;


@Component
public class TraderBot extends Bot {

    /**
     * Slack token from application.properties file. You can get your slack token
     * next <a href="https://my.slack.com/services/new/bot">creating a new bot</a>.
     */
    @Value("${slackBotToken}")
    private String slackToken;

    private TAccount account = TAccount.getInstance();

    @Override
    public String getSlackToken() {
        return slackToken;
    }

    @Override
    public Bot getSlackBot() {
        return this;
    }

    /**
     * Invoked when the bot receives a direct mention (@botname: message)
     * or a direct message. NOTE: These two event types are added by jbot
     * to make your task easier, Slack doesn't have any direct way to
     * determine these type of events.
     *
     * @param session
     * @param event
     */
    @Controller(events = {EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE})
    public void onReceiveDM(WebSocketSession session, Event event) {

        String text = event.getText();

        RichMessage response = new RichMessage("Sorry, don't understand what you are looking for.");;

        // TODO: Create a generic response creator object which is the one that has access to the account

        if(text.equals("quote") || text.equals("q")) {
            response = RTypeCommand.getQuote(account);
        } else if(text.equals("contracts") || text.equals("c")) {
            response = RTypeCommand.getContracts(account);
        } else if(text.equals("positions") || text.equals("p")) {
            response = RTypeCommand.getPositions(account);
        } else if(text.equals("orders") || text.equals("o")) {
            response = RTypeCommand.getOrders(account);
        }

        reply(session, event, new Message(response.getText()));
    }
}