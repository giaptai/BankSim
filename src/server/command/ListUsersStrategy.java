package server.command;

import java.util.List;

import server.ChatHub;
import server.ClientHandler;

public class ListUsersStrategy implements CommandStrategy {

    @Override
    public void execute(ClientHandler sender, String args) {
        List<String> onlineClients = ChatHub.getInstance().getOnlineClient();
        StringBuilder sb = new StringBuilder();
        sb.append("Active Client " + "(" + onlineClients.size() + "):");
        for (int i = 0; i < onlineClients.size(); i++) {
        }
        sender.notifyMessage(sb.toString());
    }
}
