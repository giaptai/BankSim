package server.command;

import java.util.List;

import server.ChatHub;
import server.TextHandler;

public class ListUsersStrategy implements CommandStrategy {

    @Override
    public void execute(TextHandler sender, String args) {
        List<String> onlineClients = ChatHub.getInstance().getOnlineClient();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Active Client (%d):\n", onlineClients.size()));
        for (int i = 0; i < onlineClients.size(); i++) {
            sb.append(String.format("- %s\n", onlineClients.get(i)));
        }
        sender.notifyMessage(sb.toString());
    }
}
