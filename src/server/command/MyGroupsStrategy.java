package server.command;

import server.ChatHub;
import server.ClientHandler;

import java.util.List;
import java.lang.StringBuilder;

public class MyGroupsStrategy implements CommandStrategy {

    @Override
    public void execute(ClientHandler sender, String args) {
        String clientId = sender.getClientId();
        List<String> groups = ChatHub.getInstance().getJoinedGroups(clientId);
        if (groups.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Joined Groups (%s): \n", groups.size()));

        for (String group : groups) {
            sb.append("- ").append(group).append("\n");
        }
        sender.notifyMessage(sb.toString());
    }
}
