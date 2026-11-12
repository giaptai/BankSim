package server.command;

import server.ChatHub;
import server.ClientHandler;

public class LeaveGroupStrategy implements CommandStrategy {

    @Override
    public void execute(ClientHandler sender, String args) {
        String clientId = sender.getClientId();

        String[] groups = args.split("\\s+");
        for (String group : groups) {
            ChatHub.getInstance().leaveGroup(group, clientId);
        }
    }

}
