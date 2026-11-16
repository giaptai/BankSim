package server.command;

import server.ChatHub;
import server.TextHandler;

public class CreateGroupStrategy implements CommandStrategy {

    @Override
    public void execute(TextHandler sender, String args) {
        String groupName = args.trim();
        ChatHub.getInstance().createGroup(groupName, sender.getClientId());
    }
   
}
