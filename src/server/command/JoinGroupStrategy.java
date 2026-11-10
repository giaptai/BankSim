package server.command;

import server.ChatHub;

public class JoinGroupStrategy implements CommandStrategy {
    @Override
    public void execute(server.ClientHandler sender, String args) {
        String groupName = args;
        ChatHub.getInstance().joinGroup(groupName, sender.getClientId());
    }
}
