package server.command;

import server.ChatHub;
import server.ClientHandler;

public class GroupMsgStrategy implements CommandStrategy {
    @Override
    public void execute(ClientHandler sender, String args) {
        String[] data = args.split("\\s+", 2);
        String fromClientId = sender.getClientId();
        String groupName = data[0];
        String msg = data[1];
        ChatHub.getInstance().sendGroupMsg(fromClientId, groupName, msg);
    }
}
