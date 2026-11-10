package server.command;

import server.ChatHub;
import server.ClientHandler;

public class CreateGroupStrategy implements CommandStrategy {

    @Override
    public void execute(ClientHandler sender, String args) {
        String groupName = args.trim();
        ChatHub.getInstance().createGroup(groupName, sender.getClientId());
    }
   
}
